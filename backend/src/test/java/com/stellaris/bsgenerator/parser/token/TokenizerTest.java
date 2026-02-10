package com.stellaris.bsgenerator.parser.token;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.stellaris.bsgenerator.parser.token.TokenType.*;
import static org.junit.jupiter.api.Assertions.*;

class TokenizerTest {

    @Test
    void simpleKeyValue() {
        var tokens = Tokenizer.tokenize("cost = 2");
        assertTypes(tokens, IDENTIFIER, EQUALS, NUMBER, EOF);
        assertEquals("cost", tokens.get(0).value());
        assertEquals("2", tokens.get(2).value());
    }

    @Test
    void stringLiteral() {
        var tokens = Tokenizer.tokenize("description = \"hello world\"");
        assertTypes(tokens, IDENTIFIER, EQUALS, STRING, EOF);
        assertEquals("hello world", tokens.get(2).value());
    }

    @Test
    void nestedBraces() {
        var tokens = Tokenizer.tokenize("a = { b = 1 }");
        assertTypes(tokens, IDENTIFIER, EQUALS, OPEN_BRACE, IDENTIFIER, EQUALS, NUMBER, CLOSE_BRACE, EOF);
    }

    @Test
    void commentsSkipped() {
        var tokens = Tokenizer.tokenize("# this is a comment\nkey = val");
        assertTypes(tokens, IDENTIFIER, EQUALS, IDENTIFIER, EOF);
        assertEquals("key", tokens.get(0).value());
    }

    @Test
    void inlineComment() {
        var tokens = Tokenizer.tokenize("cost = 2 # inline");
        assertTypes(tokens, IDENTIFIER, EQUALS, NUMBER, EOF);
    }

    @Test
    void variableDefinition() {
        var tokens = Tokenizer.tokenize("@my_var = 42");
        assertTypes(tokens, VARIABLE_DEF, NUMBER, EOF);
        assertEquals("my_var", tokens.get(0).value());
    }

    @Test
    void variableReference() {
        var tokens = Tokenizer.tokenize("cost = @my_var");
        assertTypes(tokens, IDENTIFIER, EQUALS, VARIABLE_REF, EOF);
        assertEquals("my_var", tokens.get(2).value());
    }

    @Test
    void negativeNumber() {
        var tokens = Tokenizer.tokenize("value = -0.15");
        assertTypes(tokens, IDENTIFIER, EQUALS, NUMBER, EOF);
        assertEquals("-0.15", tokens.get(2).value());
        assertEquals(-0.15, tokens.get(2).asNumber(), 0.001);
    }

    @Test
    void decimalNumber() {
        var tokens = Tokenizer.tokenize("factor = 1.5");
        assertTypes(tokens, IDENTIFIER, EQUALS, NUMBER, EOF);
        assertEquals(1.5, tokens.get(2).asNumber(), 0.001);
    }

    @Test
    void yesNoAsIdentifier() {
        var tokens = Tokenizer.tokenize("is_origin = yes\npickable = no");
        assertTypes(tokens, IDENTIFIER, EQUALS, IDENTIFIER, IDENTIFIER, EQUALS, IDENTIFIER, EOF);
        assertTrue(tokens.get(2).isIdentifier("yes"));
        assertTrue(tokens.get(5).isIdentifier("no"));
    }

    @Test
    void comparisonOperators() {
        var tokens = Tokenizer.tokenize("count > 0\ncount < 5\ncount >= 2\ncount <= 3");
        assertTypes(tokens,
                IDENTIFIER, COMPARISON, NUMBER,
                IDENTIFIER, COMPARISON, NUMBER,
                IDENTIFIER, COMPARISON, NUMBER,
                IDENTIFIER, COMPARISON, NUMBER,
                EOF);
        assertEquals(">", tokens.get(1).value());
        assertEquals("<", tokens.get(4).value());
        assertEquals(">=", tokens.get(7).value());
        assertEquals("<=", tokens.get(10).value());
    }

    @Test
    void bareValues() {
        var tokens = Tokenizer.tokenize("tags = { TAG_A TAG_B TAG_C }");
        assertTypes(tokens, IDENTIFIER, EQUALS, OPEN_BRACE,
                IDENTIFIER, IDENTIFIER, IDENTIFIER,
                CLOSE_BRACE, EOF);
    }

    @Test
    void identifierWithSpecialChars() {
        var tokens = Tokenizer.tokenize("ethic_fanatic_authoritarian = { }");
        assertTypes(tokens, IDENTIFIER, EQUALS, OPEN_BRACE, CLOSE_BRACE, EOF);
        assertEquals("ethic_fanatic_authoritarian", tokens.get(0).value());
    }

    @Test
    void lineAndColumnTracking() {
        var tokens = Tokenizer.tokenize("a = 1\nb = 2");
        assertEquals(1, tokens.get(0).line());
        assertEquals(1, tokens.get(0).column());
        assertEquals(3, tokens.get(1).column()); // '='
        assertEquals(2, tokens.get(3).line());    // 'b'
        assertEquals(1, tokens.get(3).column());
    }

    @Test
    void unterminatedStringThrows() {
        assertThrows(TokenizerException.class, () -> Tokenizer.tokenize("x = \"no end"));
    }

    @Test
    void emptyInput() {
        var tokens = Tokenizer.tokenize("");
        assertEquals(1, tokens.size());
        assertEquals(EOF, tokens.getFirst().type());
    }

    @Test
    void realEthicsSnippet() {
        String input = """
                ethic_fanatic_authoritarian = {
                    cost = 2
                    category = "col"
                    use_for_pops = no
                    regular_variant = ethic_authoritarian
                    tags = {
                        ETHIC_ALLOWS_STRATIFIED_SOCIETY
                        ETHIC_ALLOWS_SLAVERY
                    }
                    random_weight = {
                        base = 150
                    }
                }
                """;
        var tokens = Tokenizer.tokenize(input);
        // Should start with identifier for the ethic name
        assertEquals("ethic_fanatic_authoritarian", tokens.getFirst().value());
        // Should end with EOF
        assertEquals(EOF, tokens.getLast().type());
        // Should have no errors — just verify count is reasonable
        assertTrue(tokens.size() > 20);
    }

    @Test
    void variableInBlock() {
        String input = """
                random_weight = { base = @civic_default_random_weight }
                """;
        var tokens = Tokenizer.tokenize(input);
        assertTypes(tokens, IDENTIFIER, EQUALS, OPEN_BRACE,
                IDENTIFIER, EQUALS, VARIABLE_REF,
                CLOSE_BRACE, EOF);
        assertEquals("civic_default_random_weight", tokens.get(5).value());
    }

    private void assertTypes(List<Token> tokens, TokenType... expected) {
        assertEquals(expected.length, tokens.size(),
                "Token count mismatch. Got: " + tokens.stream().map(Token::type).toList());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], tokens.get(i).type(),
                    "Token " + i + ": expected " + expected[i] + " but got " + tokens.get(i));
        }
    }
}
