package com.javagenai.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag")
public record RagSettings(
        int passageCharacterLimit,
        int passageOverlapCharacters,
        int vectorHitCount,
        int lexicalHitCount,
        int reciprocalRankConstant,
        int rerankPoolSize,
        int passagesInPrompt,
        String crossEncoderDirectory) {

    public RagSettings {
        if (passageCharacterLimit < 200) {
            throw new IllegalArgumentException("passageCharacterLimit must be at least 200");
        }
        if (passageOverlapCharacters < 0 || passageOverlapCharacters >= passageCharacterLimit) {
            throw new IllegalArgumentException("passageOverlapCharacters must be >= 0 and smaller than the window");
        }
        if (vectorHitCount < 1 || lexicalHitCount < 1 || rerankPoolSize < 1 || passagesInPrompt < 1) {
            throw new IllegalArgumentException("retrieval counts must be at least 1");
        }
        if (passagesInPrompt > rerankPoolSize) {
            throw new IllegalArgumentException("passagesInPrompt cannot exceed rerankPoolSize");
        }
        if (reciprocalRankConstant < 1) {
            throw new IllegalArgumentException("reciprocalRankConstant must be at least 1");
        }
        if (crossEncoderDirectory == null || crossEncoderDirectory.isBlank()) {
            throw new IllegalArgumentException("crossEncoderDirectory is required");
        }
    }
}
