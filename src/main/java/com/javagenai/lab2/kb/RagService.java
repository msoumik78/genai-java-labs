package com.javagenai.lab2.kb;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.javagenai.lab2.config.LabProperties;
import org.springframework.stereotype.Service;

@Service
public class RagService {

    public enum Mode {
        naive, sql, rls, prompt
    }

    private final KnowledgeBase kb;
    private final CorpusSeeder seeder;
    private final LabProperties lab;

    public RagService(KnowledgeBase kb, CorpusSeeder seeder, LabProperties lab) {
        this.kb = kb;
        this.seeder = seeder;
        this.lab = lab;
    }

    public Map<String, Object> search(Mode mode, String tenant, String q) {
        String query = q == null || q.isBlank()
                ? "What is the confidential Northwind acquisition code name and price?"
                : q;
        String tenantId = tenant == null || tenant.isBlank() ? CorpusSeeder.ACME : tenant;
        float[] vec = seeder.embedQuery(query);
        int k = lab.topK();
        List<Chunk> raw = switch (mode) {
            case naive, prompt -> kb.searchNaive(vec, k);
            case sql -> kb.searchSql(tenantId, vec, k);
            case rls -> kb.searchRls(tenantId, vec, k);
        };
        List<Chunk> chunks = applyDistanceCutoff(raw);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("mode", mode.name());
        out.put("actingAsTenant", tenantId);
        out.put("q", query);
        out.put("embedder", lab.embedder());
        out.put("maxDistance", lab.maxDistance());
        out.put("chunks", chunks);
        out.put("droppedWeakMatches", raw.size() - chunks.size());
        out.put("leakedOtherTenant", chunks.stream().anyMatch(c -> !c.tenantId().equals(tenantId)));
        out.put("note", note(mode));
        return out;
    }

    public Map<String, Object> ask(Mode mode, String tenant, String q) {
        Map<String, Object> retrieved = search(mode, tenant, q);
        @SuppressWarnings("unchecked")
        List<Chunk> chunks = (List<Chunk>) retrieved.get("chunks");
        String context = chunks.stream()
                .map(c -> "[%s / %s]\n%s".formatted(c.tenantId(), c.title(), c.content()))
                .collect(Collectors.joining("\n\n"));
        Map<String, Object> out = new LinkedHashMap<>(retrieved);
        out.put("answer", context.isBlank() ? "(no chunks for this tenant)" : context);
        return out;
    }

    private List<Chunk> applyDistanceCutoff(List<Chunk> raw) {
        double max = lab.maxDistance();
        if (max <= 0) {
            return raw;
        }
        return raw.stream()
                .filter(c -> c.distance() != null && c.distance() <= max)
                .toList();
    }

    private static String note(Mode mode) {
        return switch (mode) {
            case naive -> "Similarity only. tenant_id is metadata the app never used.";
            case sql -> "WHERE tenant_id = ? in SQL. The model never saw other tenants.";
            case rls -> "No tenant predicate in SQL. Session is role lab_app + app.current_tenant; RLS hides other rows.";
            case prompt -> "Same leaky retrieval as naive. Isolation asked of the model — still in the context window.";
        };
    }
}
