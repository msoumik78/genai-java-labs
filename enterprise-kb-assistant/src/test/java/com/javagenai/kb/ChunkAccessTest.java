package com.javagenai.kb.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class ChunkAccessTest {

    @Test
    void aRoleOnTheListMayRead() {
        Chunk chunk = chunkWith(List.of("EMPLOYEE", "MANAGER"));

        assertThat(chunk.isVisibleTo("MANAGER")).isTrue();
    }

    @Test
    void aRoleMissingFromTheListIsRefused() {
        Chunk chunk = chunkWith(List.of("MANAGER"));

        assertThat(chunk.isVisibleTo("EMPLOYEE")).isFalse();
    }

    @Test
    void anEmptyRoleListMeansPubliclyReadable() {
        Chunk chunk = chunkWith(List.of());

        assertThat(chunk.isVisibleTo("ANYONE")).isTrue();
    }

    @Test
    void rejectsEmptyText() {
        assertThatThrownBy(() -> new Chunk("c1", "d1", "s1", "  ", "", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Chunk chunkWith(List<String> roles) {
        return new Chunk("c1", "d1", "s1", "Salary bands are published twice yearly.",
                "Compensation", roles);
    }
}
