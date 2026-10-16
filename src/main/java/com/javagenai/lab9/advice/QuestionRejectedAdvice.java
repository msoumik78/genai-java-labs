package com.javagenai.lab9.advice;

import java.util.Map;

import com.javagenai.lab9.exceptions.QuestionRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class QuestionRejectedAdvice {

    @ExceptionHandler(QuestionRejectedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, String> rejected(QuestionRejectedException ex) {
        return Map.of(
                "error", "question rejected before a prompt was built",
                "ruleId", ex.ruleId());
    }
}
