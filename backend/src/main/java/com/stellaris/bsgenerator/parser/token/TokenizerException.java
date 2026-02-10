package com.stellaris.bsgenerator.parser.token;

public class TokenizerException extends RuntimeException {

    private final int line;
    private final int column;

    public TokenizerException(String message, int line, int column) {
        super(message + " at line " + line + ", column " + column);
        this.line = line;
        this.column = column;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }
}
