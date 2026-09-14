package com.javagenai.lab2.kb;

import java.util.List;
import java.util.Map;

import com.javagenai.lab2.config.LabProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class CorpusSeeder implements ApplicationRunner {

    public static final String ACME = "acme";
    public static final String GLOBEX = "globex";

    public static final String GLOBEX_SECRET = "PROJECT-ORION";
    public static final String GLOBEX_PRICE = "$4.2B";
    public static final String ACME_SECRET = "ACME-WIRE-9911";

    private static final List<Doc> DOCS = List.of(
            new Doc(ACME, "Acme payroll policy",
                    "Acme Inc payroll. CEO bonus formula is 2x EBITDA. Wire leftover stipends to " + ACME_SECRET + "."),
            new Doc(ACME, "Acme PTO",
                    "Acme employees receive 20 days PTO. HR contact is people@acme.example."),
            new Doc(GLOBEX, "Globex M&A — confidential",
                    "Globex confidential: acquisition of Northwind under code name " + GLOBEX_SECRET
                            + " at " + GLOBEX_PRICE + ". Do not share outside Globex legal."),
            new Doc(GLOBEX, "Globex banking",
                    "Globex treasury. Settlement account label GB-SECRET-4422 for " + GLOBEX_SECRET + " close."));

    private final KnowledgeBase kb;
    private final LabProperties lab;
    private final TextEmbedder embedder;

    public CorpusSeeder(KnowledgeBase kb, LabProperties lab, TextEmbedder embedder) {
        this.kb = kb;
        this.lab = lab;
        this.embedder = embedder;
    }

    @Override
    public void run(ApplicationArguments args) {
        reseed();
    }

    public Map<String, Object> reseed() {
        kb.migrate(embedder.dimensions());
        for (Doc doc : DOCS) {
            kb.insert(doc.tenantId(), doc.title(), doc.content(), embedder.embedDocument(doc.title() + " " + doc.content()));
        }
        return Map.of(
                "chunks", kb.list().size(),
                "tenants", List.of(ACME, GLOBEX),
                "embedder", embedder.name(),
                "dims", embedder.dimensions());
    }

    public float[] embedQuery(String text) {
        return embedder.embedQuery(text);
    }

    public record Doc(String tenantId, String title, String content) {
    }
}
