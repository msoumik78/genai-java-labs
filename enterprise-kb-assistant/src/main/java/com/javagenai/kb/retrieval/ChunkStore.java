package com.javagenai.kb.retrieval;

import com.javagenai.kb.domain.Chunk;
import com.javagenai.kb.ingestion.IngestedDocument;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Both retrieval arms in one place: pgvector cosine for meaning, Postgres
 * full-text for rare words and identifiers. The role filter is part of the
 * WHERE clause in both queries, so forbidden rows are excluded before ranking
 * rather than after — filtering afterwards would leak their existence through
 * result counts.
 */
@ApplicationScoped
public class ChunkStore {

    /**
     * The ACL predicate; the demo passes a null role to see the unfiltered pool
     * ("what would this query hit without access control?").
     */
    private static final String ACL = "(cardinality(roles) = 0 OR roles @> ?)";

    private static final String VECTOR_SEARCH = """
            SELECT id, document_id, section_id, section_path, body, roles
            FROM chunks
            WHERE %s
            ORDER BY embedding <=> ?::vector
            LIMIT ?
            """;

    private static final String LEXICAL_SEARCH = """
            SELECT id, document_id, section_id, section_path, body, roles
            FROM chunks
            WHERE %s AND search_vector @@ plainto_tsquery('english', ?)
            ORDER BY ts_rank_cd(search_vector, plainto_tsquery('english', ?)) DESC
            LIMIT ?
            """;

    @Inject
    AgroalDataSource dataSource;

    /** Nearest neighbours by cosine distance, restricted to the caller's roles. */
    public List<Chunk> searchByVector(float[] queryVector, String role, int limit) {
        return query(vectorSql(role), (statement, connection) -> {
            bindRoles(statement, connection, role);
            statement.setString(role == null ? 1 : 2, toVectorLiteral(queryVector));
            statement.setInt(role == null ? 2 : 3, limit);
        });
    }

    /** BM25-style lexical hits for terms a dense model tends to blur. */
    public List<Chunk> searchLexically(String query, String role, int limit) {
        return query(lexicalSql(role), (statement, connection) -> {
            bindRoles(statement, connection, role);
            int offset = role == null ? 0 : 1;
            statement.setString(1 + offset, query);
            statement.setString(2 + offset, query);
            statement.setInt(3 + offset, limit);
        });
    }

    /** Replaces the document row and its chunks in one transaction: a re-ingest is a clean slate. */
    public void save(IngestedDocument document, List<Chunk> chunks, List<float[]> vectors) {
        if (chunks.size() != vectors.size()) {
            throw new IllegalArgumentException("chunks and vectors must align");
        }
        try (Connection connection = dataSource.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                replaceDocument(connection, document);
                for (int i = 0; i < chunks.size(); i++) {
                    insertChunk(connection, chunks.get(i), vectors.get(i));
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save " + document.documentId(), e);
        }
    }

    private void replaceDocument(Connection connection, IngestedDocument document) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement("DELETE FROM documents WHERE id = ?")) {
            delete.setString(1, document.documentId());
            delete.executeUpdate();
        }
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO documents
                    (id, title, filename, allowed_roles, page_count, character_count,
                     table_count, parser, content_digest)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            insert.setString(1, document.documentId());
            insert.setString(2, document.title());
            insert.setString(3, document.filename());
            insert.setArray(4, connection.createArrayOf("text",
                    document.allowedRoles().toArray(new String[0])));
            insert.setInt(5, document.structured().stats().pageCount());
            insert.setInt(6, document.structured().stats().characterCount());
            insert.setInt(7, document.structured().stats().tableCount());
            insert.setString(8, document.parser());
            insert.setString(9, document.contentDigest());
            insert.executeUpdate();
        }
    }

    private void insertChunk(Connection connection, Chunk chunk, float[] vector) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO chunks (id, document_id, section_id, section_path, body, roles, embedding)
                VALUES (?, ?, ?, ?, ?, ?, ?::vector)
                """)) {
            insert.setString(1, chunk.id());
            insert.setString(2, chunk.documentId());
            insert.setString(3, chunk.sectionId());
            insert.setString(4, chunk.sectionPath());
            insert.setString(5, chunk.text());
            insert.setArray(6, connection.createArrayOf("text",
                    chunk.allowedRoles().toArray(new String[0])));
            insert.setString(7, toVectorLiteral(vector));
            insert.executeUpdate();
        }
    }

    private String vectorSql(String role) {
        return VECTOR_SEARCH.formatted(role == null ? "TRUE" : ACL);
    }

    private String lexicalSql(String role) {
        return LEXICAL_SEARCH.formatted(role == null ? "TRUE" : ACL);
    }

    private void bindRoles(PreparedStatement statement, Connection connection, String role)
            throws SQLException {
        if (role != null) {
            statement.setArray(1, rolesOf(connection, role));
        }
    }

    private List<Chunk> query(String sql, Bind bind) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            bind.apply(statement, connection);
            try (ResultSet rows = statement.executeQuery()) {
                return readChunks(rows);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Retrieval query failed: " + e.getMessage(), e);
        }
    }

    private List<Chunk> readChunks(ResultSet rows) throws SQLException {
        List<Chunk> chunks = new ArrayList<>();
        while (rows.next()) {
            chunks.add(new Chunk(
                    rows.getString("id"),
                    rows.getString("document_id"),
                    rows.getString("section_id"),
                    rows.getString("body"),
                    rows.getString("section_path"),
                    rolesList(rows.getArray("roles"))));
        }
        return chunks;
    }

    private List<String> rolesList(java.sql.Array roles) throws SQLException {
        if (roles == null) {
            return List.of();
        }
        return List.of((String[]) roles.getArray());
    }

    private java.sql.Array rolesOf(Connection connection, String role) throws SQLException {
        return connection.createArrayOf("text", new String[] {role});
    }

    /** pgvector parses the bracket form directly: "[0.1, 0.2, ...]". */
    static String toVectorLiteral(float[] vector) {
        StringBuilder literal = new StringBuilder(vector.length * 12).append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                literal.append(',');
            }
            literal.append(vector[i]);
        }
        return literal.append(']').toString();
    }

    @FunctionalInterface
    private interface Bind {
        void apply(PreparedStatement statement, Connection connection) throws SQLException;
    }
}