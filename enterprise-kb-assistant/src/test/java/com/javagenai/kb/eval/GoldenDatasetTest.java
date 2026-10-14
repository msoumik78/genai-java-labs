package com.javagenai.kb.eval;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Loads the committed golden dataset itself: the only test that catches schema
 * drift between the JSONL file and the record (the snake_case mistake did).
 */
class GoldenDatasetTest {

    private static final Path GOLDEN = Path.of("eval", "dataset", "golden.jsonl");

    @Test
    void loadsEveryCommittedCase() {
        List<GoldenCase> cases = GoldenDataset.load(GOLDEN);

        assertThat(cases).hasSizeGreaterThanOrEqualTo(30);
        assertThat(cases).allSatisfy(golden -> {
            assertThat(golden.id()).isNotBlank();
            assertThat(golden.question()).isNotBlank();
            assertThat(golden.category()).isNotBlank();
        });
    }

    @Test
    void snakeCaseColumnsMapOntoTheRecord() {
        GoldenCase adoption = GoldenDataset.load(GOLDEN).stream()
                .filter(item -> item.id().equals("leave-04"))
                .findFirst()
                .orElseThrow();

        assertThat(adoption.supportingDocs()).containsExactly("parental-leave");
        assertThat(adoption.mustContain()).contains("16 weeks", "placement");
        assertThat(adoption.expectsAbstain()).isFalse();
    }

    @Test
    void abstentionCasesParseTheFlag() {
        GoldenCase aclCase = GoldenDataset.load(GOLDEN).stream()
                .filter(item -> item.id().equals("acl-01"))
                .findFirst()
                .orElseThrow();

        assertThat(aclCase.expectsAbstain()).isTrue();
        assertThat(aclCase.supportingDocs()).isEmpty();
    }
}