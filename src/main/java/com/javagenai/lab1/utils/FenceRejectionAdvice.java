package com.javagenai.lab1.utils;

import java.util.concurrent.RejectedExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class FenceRejectionAdvice {

    @ExceptionHandler(RejectedExecutionException.class)
    ResponseStatusException fenceFull(RejectedExecutionException ex) {
        return new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "LLM fence is full — chat sheds load; other APIs stay up");
    }
}
