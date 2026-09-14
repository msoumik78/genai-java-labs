package com.javagenai.lab2.kb;

import java.util.UUID;

public record Chunk(UUID id, String tenantId, String title, String content, Double distance) {
}
