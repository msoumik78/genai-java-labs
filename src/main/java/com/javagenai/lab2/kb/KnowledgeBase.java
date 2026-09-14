package com.javagenai.lab2.kb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class KnowledgeBase {

    /** Non-superuser role: RLS actually applies. Superusers ignore RLS. */
    static final String APP_ROLE = "lab_app";

    private static final RowMapper<Chunk> CHUNK = (rs, n) -> new Chunk(
            rs.getObject("id", UUID.class),
            rs.getString("tenant_id"),
            rs.getString("title"),
            rs.getString("content"),
            rs.getObject("distance") != null ? rs.getDouble("distance") : null);

    private final JdbcTemplate jdbc;

    public KnowledgeBase(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void migrate(int dims) {
        jdbc.execute("CREATE EXTENSION IF NOT EXISTS vector");
        jdbc.execute("DROP TABLE IF EXISTS kb_chunk");
        jdbc.execute("""
                CREATE TABLE kb_chunk (
                  id uuid PRIMARY KEY,
                  tenant_id text NOT NULL,
                  title text NOT NULL,
                  content text NOT NULL,
                  embedding vector(%d) NOT NULL
                )
                """.formatted(dims));
        jdbc.execute("""
                DO $$ BEGIN
                  CREATE ROLE %s NOSUPERUSER NOBYPASSRLS NOLOGIN;
                EXCEPTION WHEN duplicate_object THEN
                  NULL;
                END $$
                """.formatted(APP_ROLE));
        jdbc.execute("GRANT %s TO CURRENT_USER".formatted(APP_ROLE));
        jdbc.execute("GRANT SELECT ON kb_chunk TO %s".formatted(APP_ROLE));
        jdbc.execute("ALTER TABLE kb_chunk ENABLE ROW LEVEL SECURITY");
        jdbc.execute("DROP POLICY IF EXISTS tenant_isolation ON kb_chunk");
        jdbc.execute("""
                CREATE POLICY tenant_isolation ON kb_chunk
                  USING (tenant_id = current_setting('app.current_tenant', true))
                  WITH CHECK (tenant_id = current_setting('app.current_tenant', true))
                """);
    }

    public void insert(String tenantId, String title, String content, float[] embedding) {
        jdbc.update(
                """
                INSERT INTO kb_chunk (id, tenant_id, title, content, embedding)
                VALUES (?, ?, ?, ?, CAST(? AS vector))
                """,
                UUID.randomUUID(),
                tenantId,
                title,
                content,
                Vectors.toLiteral(embedding));
    }

    public List<Chunk> list() {
        return jdbc.query(
                "SELECT id, tenant_id, title, content, NULL::float8 AS distance FROM kb_chunk ORDER BY tenant_id, title",
                CHUNK);
    }

    /** Broken vector store: nearest neighbors, no tenant predicate. Superuser sees every row. */
    public List<Chunk> searchNaive(float[] query, int k) {
        return similarity(query, k, false, null);
    }

    /** Isolation in SQL: bind tenant_id. Not a prompt instruction. */
    public List<Chunk> searchSql(String tenantId, float[] query, int k) {
        return similarity(query, k, true, tenantId);
    }

    /**
     * Isolation in Postgres RLS. Query has no tenant_id predicate.
     * Runs as role lab_app because the default JDBC user is a superuser
     * and superusers skip RLS even with FORCE ROW LEVEL SECURITY.
     */
    public List<Chunk> searchRls(String tenantId, float[] query, int k) {
        String vector = Vectors.toLiteral(query);
        return jdbc.execute((Connection con) -> {
            con.setAutoCommit(false);
            try (var st = con.createStatement()) {
                st.execute("SET LOCAL ROLE " + APP_ROLE);
            }
            try (PreparedStatement cfg = con.prepareStatement("SELECT set_config('app.current_tenant', ?, true)")) {
                cfg.setString(1, tenantId);
                cfg.execute();
            }
            try {
                return knn(con, vector, k, false, null);
            } finally {
                con.commit();
            }
        });
    }

    private List<Chunk> similarity(float[] query, int k, boolean filterTenant, String tenantId) {
        String vector = Vectors.toLiteral(query);
        return jdbc.execute((Connection con) -> knn(con, vector, k, filterTenant, tenantId));
    }

    private static List<Chunk> knn(
            Connection con, String vector, int k, boolean filterTenant, String tenantId)
            throws java.sql.SQLException {
        String sql = filterTenant
                ? """
                SELECT id, tenant_id, title, content,
                       (embedding <=> CAST(? AS vector)) AS distance
                FROM kb_chunk
                WHERE tenant_id = ?
                ORDER BY embedding <=> CAST(? AS vector)
                LIMIT ?
                """
                : """
                SELECT id, tenant_id, title, content,
                       (embedding <=> CAST(? AS vector)) AS distance
                FROM kb_chunk
                ORDER BY embedding <=> CAST(? AS vector)
                LIMIT ?
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (filterTenant) {
                ps.setString(1, vector);
                ps.setString(2, tenantId);
                ps.setString(3, vector);
                ps.setInt(4, k);
            } else {
                ps.setString(1, vector);
                ps.setString(2, vector);
                ps.setInt(3, k);
            }
            try (ResultSet rs = ps.executeQuery()) {
                ArrayList<Chunk> out = new ArrayList<>();
                int i = 0;
                while (rs.next()) {
                    out.add(CHUNK.mapRow(rs, i++));
                }
                return out;
            }
        }
    }
}
