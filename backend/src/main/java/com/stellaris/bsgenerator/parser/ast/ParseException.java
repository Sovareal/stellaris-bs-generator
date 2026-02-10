package com.stellaris.bsgenerator.parser.ast;

import com.stellaris.bsgenerator.parser.token.Token;

public class ParseException extends RuntimeException {

    private final Token token;

    public ParseException(String message, Token token) {
        super(message + " at " + token);
        this.token = token;
    }

    public Token getToken() {
        return token;
    }
}
