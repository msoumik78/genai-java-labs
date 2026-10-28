package com.javagenai.lab10.task;

public record TaskEvent(String taskId, TaskState state, String detail) {
}
