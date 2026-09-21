package com.javagenai.lab3;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

public record Cli(String task, String text, String context) {

    private static final Set<String> TASKS = Set.of(
            "sentiment", "classification", "ner", "qa", "embed", "embedding", "fill-mask", "mask", "help");

    public static Cli parse(String[] args) {
        if (args.length == 0) {
            return new Cli("sentiment", defaults("sentiment"), null);
        }
        String task = args[0].toLowerCase(Locale.ROOT);
        if (!TASKS.contains(task)) {
            return new Cli("sentiment", String.join(" ", args), null);
        }
        if ("help".equals(task)) {
            return new Cli("help", "", null);
        }
        if ("classification".equals(task)) {
            task = "sentiment";
        }
        if ("embedding".equals(task)) {
            task = "embed";
        }
        if ("mask".equals(task)) {
            task = "fill-mask";
        }
        String rest = args.length == 1 ? defaults(task) : String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if ("qa".equals(task)) {
            int bar = rest.indexOf('|');
            if (bar < 0) {
                return new Cli("qa", rest, defaults("qa-context"));
            }
            return new Cli("qa", rest.substring(0, bar).trim(), rest.substring(bar + 1).trim());
        }
        return new Cli(task, rest, null);
    }

    public static String usage() {
        return """
                usage: <main> [task] [text...]
                  task: sentiment | ner | qa | embed | fill-mask
                  qa:   qa 'question' | 'context paragraph'
                """;
    }

    private static String defaults(String task) {
        return switch (task) {
            case "ner" -> "Apple is buying a U.K. startup for $1 billion. Tim Cook spoke in London.";
            case "qa" -> "Where will the masterclass start?";
            case "qa-context" -> "The GenAI Java masterclass starts in Bangalore on 10 October 2026.";
            case "embed" -> "Java on Apple Silicon is pleasant for encoder models.";
            case "fill-mask" -> "Paris is the [MASK] of France.";
            default -> "I am worried about when our AI Masterclass will start";
        };
    }
}
