package com.stellaris.bsgenerator.engine;

/**
 * Thrown when empire generation cannot find a valid combination.
 */
public class GenerationException extends RuntimeException {
    public GenerationException(String message) {
        super(message);
    }
}
