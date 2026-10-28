package com.javagenai.lab10.task;

public enum TaskState {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED;

    public boolean canTransitionTo(TaskState next) {
        return switch (this) {
            case PENDING -> next == RUNNING || next == CANCELLED;
            case RUNNING -> next == COMPLETED || next == FAILED || next == CANCELLED;
            case COMPLETED, FAILED, CANCELLED -> false;
        };
    }
}
