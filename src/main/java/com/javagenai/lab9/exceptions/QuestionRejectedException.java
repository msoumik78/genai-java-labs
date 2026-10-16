package com.javagenai.lab9.exceptions;

public class QuestionRejectedException extends RuntimeException {

    private final String ruleId;

    public QuestionRejectedException(String ruleId) {
        super("question rejected by rule " + ruleId);
        this.ruleId = ruleId;
    }

    public String ruleId() {
        return ruleId;
    }
}
