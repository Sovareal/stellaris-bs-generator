package com.stellaris.bsgenerator.parser.ast;

import com.stellaris.bsgenerator.parser.token.Token;
import com.stellaris.bsgenerator.parser.token.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ClausewitzParser {

    private final List<Token> tokens;
    private final Map<String, String> variables;
    private int pos;

    private ClausewitzParser(List<Token> tokens, Map<String, String> variables) {
        this.tokens = tokens;
        this.variables = variables;
        this.pos = 0;
    }

    public static ClausewitzNode parse(List<Token> tokens, Map<String, String> variables) {
        return new ClausewitzParser(tokens, variables).parseRoot();
    }

    private ClausewitzNode parseRoot() {
        var children = parseEntries();
        expect(TokenType.EOF);
        return ClausewitzNode.root(children);
    }

    private List<ClausewitzNode> parseEntries() {
        var entries = new ArrayList<ClausewitzNode>();
        while (!atEnd() && current().type() != TokenType.CLOSE_BRACE) {
            entries.add(parseEntry());
        }
        return entries;
    }

    private ClausewitzNode parseEntry() {
        Token token = current();

        // Variable definition: @var = value (already consumed '=' in tokenizer)
        if (token.type() == TokenType.VARIABLE_DEF) {
            advance();
            String varValue = parseScalarValue();
            variables.put(token.value(), varValue);
            // Variable defs don't produce AST nodes
            return parseEntry();
        }

        // Variable reference as bare value
        if (token.type() == TokenType.VARIABLE_REF) {
            advance();
            String resolved = resolveVariable(token);
            return ClausewitzNode.bareValue(resolved);
        }

        // Check if this is a key=value / key={block} or a bare value
        if (isKeyToken(token) && hasLookahead()) {
            Token next = tokens.get(pos + 1);

            // key = ...
            if (next.type() == TokenType.EQUALS) {
                return parseKeyValue();
            }

            // key > value, key < value, key >= value, key <= value
            if (next.type() == TokenType.COMPARISON) {
                return parseComparison();
            }
        }

        // Bare value (identifier, string, or number without a key)
        advance();
        return ClausewitzNode.bareValue(scalarOf(token));
    }

    private ClausewitzNode parseKeyValue() {
        Token keyToken = current();
        String key = scalarOf(keyToken);
        advance(); // key
        advance(); // =

        Token valToken = current();

        // Block: key = { ... }
        if (valToken.type() == TokenType.OPEN_BRACE) {
            advance(); // {
            var children = parseEntries();
            expect(TokenType.CLOSE_BRACE);
            advance(); // }
            return ClausewitzNode.block(key, children);
        }

        // Scalar: key = value
        String value = parseScalarValue();
        return ClausewitzNode.leaf(key, value);
    }

    private ClausewitzNode parseComparison() {
        Token keyToken = current();
        String key = scalarOf(keyToken);
        advance(); // key
        Token op = current();
        advance(); // comparison operator
        String value = parseScalarValue();
        return ClausewitzNode.leaf(key, op.value() + " " + value);
    }

    private String parseScalarValue() {
        Token token = current();
        advance();
        if (token.type() == TokenType.VARIABLE_REF) {
            return resolveVariable(token);
        }
        return scalarOf(token);
    }

    private String resolveVariable(Token varToken) {
        String resolved = variables.get(varToken.value());
        if (resolved == null) {
            throw new ParseException("Undefined variable @" + varToken.value(), varToken);
        }
        return resolved;
    }

    private String scalarOf(Token token) {
        return token.value();
    }

    private boolean isKeyToken(Token token) {
        return token.type() == TokenType.IDENTIFIER
                || token.type() == TokenType.STRING
                || token.type() == TokenType.NUMBER;
    }

    private boolean hasLookahead() {
        return pos + 1 < tokens.size();
    }

    private Token current() {
        return tokens.get(pos);
    }

    private boolean atEnd() {
        return pos >= tokens.size() || tokens.get(pos).type() == TokenType.EOF;
    }

    private void advance() {
        if (!atEnd()) pos++;
    }

    private void expect(TokenType type) {
        if (current().type() != type) {
            throw new ParseException("Expected " + type + " but got " + current().type(), current());
        }
    }
}
