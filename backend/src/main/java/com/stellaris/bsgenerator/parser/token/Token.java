package com.stellaris.bsgenerator.parser.token;

public record Token(TokenType type, String value, int line, int column) {

    public double asNumber() {
        return Double.parseDouble(value);
    }

    public int asInt() {
        return (int) asNumber();
    }

    public boolean isIdentifier(String expected) {
        return type == TokenType.IDENTIFIER && value.equalsIgnoreCase(expected);
    }

    @Override
    public String toString() {
        return type + "(" + value + ") at " + line + ":" + column;
    }
}
