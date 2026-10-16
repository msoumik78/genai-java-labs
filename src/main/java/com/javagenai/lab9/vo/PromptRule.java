package com.javagenai.lab9.vo;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record PromptRule(String id, Pattern pattern) {

    public Optional<String> matchingExcerpt(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group());
    }
}
