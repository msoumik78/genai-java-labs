package com.javagenai.lab9.services;

import java.util.List;
import java.util.regex.Pattern;

import com.javagenai.lab9.vo.PromptRule;

public final class PromptRules {

    private PromptRules() {}

    public static List<PromptRule> standard() {
        return List.of(
                compiled("instruction-hijack", "ignore\\s+(all\\s+)?(previous|prior|above)\\s+instructions"),
                compiled("role-override", "you\\s+are\\s+now"),
                compiled("delimiter-breakout", "</data>|</untrusted>|\\bsystem\\s*:"),
                compiled("secret-exfil", "api\\s+key|system\\s+prompt"));
    }

    private static PromptRule compiled(String id, String regex) {
        return new PromptRule(id, Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
    }
}
