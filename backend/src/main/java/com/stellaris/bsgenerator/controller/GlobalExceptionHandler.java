package com.stellaris.bsgenerator.controller;

import com.stellaris.bsgenerator.engine.GenerationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorResponse(String error, String message) {}

    @ExceptionHandler(GenerationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleGenerationException(GenerationException e) {
        log.warn("Generation failed: {}", e.getMessage());
        return new ErrorResponse("generation_failed", e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleIllegalState(IllegalStateException e) {
        log.warn("Invalid state: {}", e.getMessage());
        return new ErrorResponse("invalid_state", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Bad request: {}", e.getMessage());
        return new ErrorResponse("bad_request", e.getMessage());
    }

    @ExceptionHandler(IOException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleIOException(IOException e) {
        log.error("IO error: {}", e.getMessage(), e);
        return new ErrorResponse("io_error", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpected(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return new ErrorResponse("internal_error", "An unexpected error occurred");
    }
}
