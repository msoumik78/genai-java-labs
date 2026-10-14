package com.javagenai.kb.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Reads and writes the evaluation tables the dashboard compares. */
@ApplicationScoped
public class JdbcEvaluationStore implements EvaluationStore {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Inject
    AgroalDataSource dataSource;

    @Override
    public void save(EvalRun run) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                insertRun(connection, run);
                for (EvalCase item : run.cases()) {
                    insertCase(connection, run.id(), item);
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not store evaluation run " + run.label(), e);
        }
    }

    @Override
    public List<EvalRun> list() {
        String sql = """
                SELECT r.id, r.label, r.git_sha, r.config::text, r.judge_model, r.corpus_digest, r.created_at,
                       c.case_id, c.question, c.user_role, c.category, c.answer, c.judged::text, c.latencies::text
                FROM evaluation_runs r
                LEFT JOIN evaluation_cases c ON c.run_id = r.id
                ORDER BY r.created_at, c.case_id
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rows = statement.executeQuery()) {
            Map<UUID, MutableRun> byId = new LinkedHashMap<>();
            while (rows.next()) {
                UUID id = rows.getObject("id", UUID.class);
                MutableRun run = byId.get(id);
                if (run == null) {
                    run = new MutableRun(
                            id,
                            rows.getString("label"),
                            rows.getString("git_sha"),
                            rows.getString("config"),
                            rows.getString("judge_model"),
                            rows.getString("corpus_digest"),
                            rows.getTimestamp("created_at").toInstant());
                    byId.put(id, run);
                }
                if (rows.getString("case_id") != null) {
                    run.cases.add(readCase(rows));
                }
            }
            return byId.values().stream()
                    .map(run -> new EvalRun(
                            run.id, run.label, run.gitSha, run.configJson, run.judgeModel,
                            run.corpusDigest, run.createdAt, run.cases))
                    .toList();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read evaluation runs", e);
        }
    }

    private void insertRun(Connection connection, EvalRun run) throws SQLException {
        String sql = """
                INSERT INTO evaluation_runs (id, label, git_sha, config, judge_model, corpus_digest, created_at)
                VALUES (?, ?, ?, ?::jsonb, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, run.id());
            statement.setString(2, run.label());
            statement.setString(3, run.gitSha());
            statement.setString(4, run.configJson() == null ? "{}" : run.configJson());
            statement.setString(5, run.judgeModel());
            statement.setString(6, run.corpusDigest());
            statement.setTimestamp(7, Timestamp.from(run.createdAt() == null ? Instant.now() : run.createdAt()));
            statement.executeUpdate();
        }
    }

    private void insertCase(Connection connection, UUID runId, EvalCase item) throws SQLException {
        String sql = """
                INSERT INTO evaluation_cases
                    (id, run_id, case_id, question, user_role, category, answer, judged, latencies)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, UUID.randomUUID());
            statement.setObject(2, runId);
            statement.setString(3, item.caseId());
            statement.setString(4, item.question());
            statement.setString(5, item.userRole());
            statement.setString(6, item.category());
            statement.setString(7, item.answer());
            statement.setString(8, judgedJson(item));
            statement.setString(9, latenciesJson(item));
            statement.executeUpdate();
        }
    }

    private EvalCase readCase(ResultSet rows) throws SQLException {
        try {
            var judged = JSON.readTree(rows.getString("judged") == null ? "{}" : rows.getString("judged"));
            var latencies = JSON.readTree(rows.getString("latencies") == null ? "{}" : rows.getString("latencies"));
            return new EvalCase(
                    rows.getString("case_id"),
                    rows.getString("question"),
                    rows.getString("user_role"),
                    rows.getString("category"),
                    rows.getString("answer"),
                    judged.path("abstained").asBoolean(false),
                    judged.path("citationsValid").asBoolean(false),
                    nullable(judged, "contextRecall"),
                    nullable(judged, "phraseHit"),
                    nullable(judged, "faithfulness"),
                    nullable(judged, "relevancy"),
                    nullable(judged, "completeness"),
                    latencies.path("latencyMs").asLong(0),
                    latencies.path("promptTokens").asInt(0),
                    latencies.path("completionTokens").asInt(0),
                    judged.path("failed").asBoolean(false));
        } catch (Exception e) {
            throw new SQLException("Unreadable evaluation case JSON", e);
        }
    }

    private static Double nullable(com.fasterxml.jackson.databind.JsonNode node, String field) {
        com.fasterxml.jackson.databind.JsonNode value = node.path(field);
        return value.isNumber() ? value.asDouble() : null;
    }

    private String judgedJson(EvalCase item) {
        ObjectNode node = JSON.createObjectNode();
        node.put("abstained", item.abstained());
        node.put("citationsValid", item.citationsValid());
        putNullable(node, "contextRecall", item.contextRecall());
        putNullable(node, "phraseHit", item.phraseHit());
        node.put("failed", item.failed());
        putNullable(node, "faithfulness", item.faithfulness());
        putNullable(node, "relevancy", item.relevancy());
        putNullable(node, "completeness", item.completeness());
        return node.toString();
    }

    private String latenciesJson(EvalCase item) {
        ObjectNode node = JSON.createObjectNode();
        node.put("latencyMs", item.latencyMs());
        node.put("promptTokens", item.promptTokens());
        node.put("completionTokens", item.completionTokens());
        return node.toString();
    }

    private static void putNullable(ObjectNode node, String field, Double value) {
        if (value == null) {
            node.putNull(field);
        } else {
            node.put(field, value);
        }
    }

    private record MutableRun(
            UUID id,
            String label,
            String gitSha,
            String configJson,
            String judgeModel,
            String corpusDigest,
            Instant createdAt,
            List<EvalCase> cases) {

        private MutableRun(
                UUID id, String label, String gitSha, String configJson,
                String judgeModel, String corpusDigest, Instant createdAt) {
            this(id, label, gitSha, configJson, judgeModel, corpusDigest, createdAt, new ArrayList<>());
        }
    }
}
