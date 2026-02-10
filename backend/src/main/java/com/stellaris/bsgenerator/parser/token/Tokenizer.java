package com.stellaris.bsgenerator.parser.token;

import java.util.ArrayList;
import java.util.List;

public final class Tokenizer {

    private final String input;
    private int pos;
    private int line;
    private int column;

    private Tokenizer(String input) {
        this.input = input;
        this.pos = 0;
        this.line = 1;
        this.column = 1;
    }

    public static List<Token> tokenize(String input) {
        return new Tokenizer(input).run();
    }

    private List<Token> run() {
        var tokens = new ArrayList<Token>();
        while (pos < input.length()) {
            skipWhitespaceAndComments();
            if (pos >= input.length()) break;

            char c = peek();
            Token token = switch (c) {
                case '{' -> single(TokenType.OPEN_BRACE);
                case '}' -> single(TokenType.CLOSE_BRACE);
                case '=' -> single(TokenType.EQUALS);
                case '"' -> readString();
                case '@' -> readVariable();
                case '<', '>' -> readComparison();
                default -> {
                    if (c == '-' && hasNext() && isDigitChar(peekAt(pos + 1))) {
                        yield readNumber();
                    }
                    if (isDigitChar(c)) {
                        yield readNumber();
                    }
                    if (isIdentStart(c)) {
                        yield readIdentifier();
                    }
                    throw new TokenizerException("Unexpected character '" + c + "'", line, column);
                }
            };
            tokens.add(token);
        }
        tokens.add(new Token(TokenType.EOF, "", line, column));
        return tokens;
    }

    private Token single(TokenType type) {
        Token t = new Token(type, String.valueOf(peek()), line, column);
        advance();
        return t;
    }

    private Token readString() {
        int startLine = line;
        int startCol = column;
        advance(); // skip opening quote
        var sb = new StringBuilder();
        while (pos < input.length() && peek() != '"') {
            if (peek() == '\\' && hasNext()) {
                advance();
                sb.append(peek());
            } else {
                sb.append(peek());
            }
            advance();
        }
        if (pos >= input.length()) {
            throw new TokenizerException("Unterminated string", startLine, startCol);
        }
        advance(); // skip closing quote
        return new Token(TokenType.STRING, sb.toString(), startLine, startCol);
    }

    private Token readVariable() {
        int startLine = line;
        int startCol = column;
        advance(); // skip @
        var sb = new StringBuilder();
        while (pos < input.length() && isIdentPart(peek())) {
            sb.append(peek());
            advance();
        }
        if (sb.isEmpty()) {
            throw new TokenizerException("Empty variable name after @", startLine, startCol);
        }
        String name = sb.toString();

        // Peek ahead past whitespace for '=' to distinguish def vs ref
        int saved = pos;
        int savedLine = line;
        int savedCol = column;
        skipWhitespaceOnly();
        if (pos < input.length() && peek() == '=') {
            advance(); // consume '='
            return new Token(TokenType.VARIABLE_DEF, name, startLine, startCol);
        }
        // Restore position — it's a reference
        pos = saved;
        line = savedLine;
        column = savedCol;
        return new Token(TokenType.VARIABLE_REF, name, startLine, startCol);
    }

    private Token readNumber() {
        int startLine = line;
        int startCol = column;
        var sb = new StringBuilder();
        if (peek() == '-') {
            sb.append('-');
            advance();
        }
        while (pos < input.length() && isDigitChar(peek())) {
            sb.append(peek());
            advance();
        }
        if (pos < input.length() && peek() == '.') {
            sb.append('.');
            advance();
            while (pos < input.length() && isDigitChar(peek())) {
                sb.append(peek());
                advance();
            }
        }
        return new Token(TokenType.NUMBER, sb.toString(), startLine, startCol);
    }

    private Token readIdentifier() {
        int startLine = line;
        int startCol = column;
        var sb = new StringBuilder();
        while (pos < input.length() && isIdentPart(peek())) {
            sb.append(peek());
            advance();
        }
        return new Token(TokenType.IDENTIFIER, sb.toString(), startLine, startCol);
    }

    private Token readComparison() {
        int startLine = line;
        int startCol = column;
        var sb = new StringBuilder();
        sb.append(peek());
        advance();
        if (pos < input.length() && peek() == '=') {
            sb.append('=');
            advance();
        }
        return new Token(TokenType.COMPARISON, sb.toString(), startLine, startCol);
    }

    private void skipWhitespaceAndComments() {
        while (pos < input.length()) {
            char c = peek();
            if (c == '#') {
                while (pos < input.length() && peek() != '\n') {
                    advance();
                }
            } else if (Character.isWhitespace(c)) {
                advance();
            } else {
                break;
            }
        }
    }

    private void skipWhitespaceOnly() {
        while (pos < input.length() && peek() != '\n' && Character.isWhitespace(peek())) {
            advance();
        }
    }

    private char peek() {
        return input.charAt(pos);
    }

    private char peekAt(int index) {
        return input.charAt(index);
    }

    private boolean hasNext() {
        return pos + 1 < input.length();
    }

    private void advance() {
        if (input.charAt(pos) == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        pos++;
    }

    private static boolean isDigitChar(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isIdentStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private static boolean isIdentPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.' || c == ':' || c == '|' || c == '\'' || c == '/';
    }
}
