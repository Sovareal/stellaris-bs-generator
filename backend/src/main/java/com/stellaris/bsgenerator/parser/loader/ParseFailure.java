package com.stellaris.bsgenerator.parser.loader;

/** A game file that could not be tokenized or parsed. {@code directory} is relative to the game's common/ dir. */
public record ParseFailure(String directory, String fileName, String message) {

    @Override
    public String toString() {
        return directory + "/" + fileName + ": " + message;
    }
}
