package com.stellaris.bsgenerator.parser.cache;

/** Game data is missing or incomplete (e.g. files the parser could not read). The message is shown to the user. */
public class GameDataLoadException extends RuntimeException {

    public GameDataLoadException(String message) {
        super(message);
    }
}
