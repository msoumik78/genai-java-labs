package com.javagenai.lab10.task;

import java.util.List;

/** One job and the capabilities the plane must run. */
public record Task(String id, String payload, List<String> route) {
    public Task {
        route = List.copyOf(route);
    }

    public static Task of(String id, String payload, String... route) {
        return new Task(id, payload, List.of(route));
    }
}
