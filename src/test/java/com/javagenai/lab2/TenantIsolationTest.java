package com.javagenai.lab2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import com.javagenai.lab2.kb.Chunk;
import com.javagenai.lab2.kb.CorpusSeeder;
import com.javagenai.lab2.kb.RagService;
import com.javagenai.lab2.kb.RagService.Mode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Import(StubEmbedderConfig.class)
@TestPropertySource(properties = {
        "spring.ai.model.embedding=none",
        "lab.embedding-dims=8",
        "lab.max-distance=2",
        "lab.embedder=stub"
})
@Testcontainers
class TenantIsolationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @Autowired
    RagService rag;

    @Test
    void naiveSearchLeaksGlobexToAcme() {
        Map<String, Object> out = rag.search(Mode.naive, CorpusSeeder.ACME, "PROJECT-ORION acquisition price");
        assertThat(out.get("leakedOtherTenant")).isEqualTo(true);
        assertThat(tenants(out)).contains(CorpusSeeder.GLOBEX);
        assertThat(blob(out)).contains(CorpusSeeder.GLOBEX_SECRET);
    }

    @Test
    void sqlFilterDoesNotLeak() {
        Map<String, Object> out = rag.search(Mode.sql, CorpusSeeder.ACME, "PROJECT-ORION acquisition price");
        assertThat(out.get("leakedOtherTenant")).isEqualTo(false);
        assertThat(blob(out)).doesNotContain(CorpusSeeder.GLOBEX_SECRET);
    }

    @Test
    void rlsWithoutWhereDoesNotLeak() {
        Map<String, Object> out = rag.search(Mode.rls, CorpusSeeder.ACME, "PROJECT-ORION acquisition price");
        assertThat(out.get("leakedOtherTenant")).isEqualTo(false);
        assertThat(blob(out)).doesNotContain(CorpusSeeder.GLOBEX_SECRET);
    }

    @Test
    void promptModeStillRetrievesTheOtherTenant() {
        Map<String, Object> out = rag.search(Mode.prompt, CorpusSeeder.ACME, "PROJECT-ORION acquisition price");
        assertThat(out.get("leakedOtherTenant")).isEqualTo(true);
    }

    @SuppressWarnings("unchecked")
    private static List<String> tenants(Map<String, Object> out) {
        return ((List<Chunk>) out.get("chunks")).stream().map(Chunk::tenantId).toList();
    }

    @SuppressWarnings("unchecked")
    private static String blob(Map<String, Object> out) {
        return ((List<Chunk>) out.get("chunks")).stream().map(Chunk::content).reduce("", String::concat);
    }
}
