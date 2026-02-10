package com.stellaris.bsgenerator.parser.loader;

import com.stellaris.bsgenerator.parser.token.Tokenizer;
import com.stellaris.bsgenerator.parser.token.Token;
import com.stellaris.bsgenerator.parser.token.TokenType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class ScriptedVariableLoader {

    private ScriptedVariableLoader() {}

    public static Map<String, String> loadFromDirectory(Path directory) throws IOException {
        var variables = new HashMap<String, String>();
        if (!Files.isDirectory(directory)) {
            return variables;
        }

        List<Path> files;
        try (Stream<Path> stream = Files.list(directory)) {
            files = stream
                    .filter(p -> p.toString().endsWith(".txt"))
                    .sorted()
                    .toList();
        }

        for (Path file : files) {
            loadFromFile(file, variables);
        }
        return variables;
    }

    private static void loadFromFile(Path file, Map<String, String> variables) throws IOException {
        String content = stripBom(Files.readString(file, StandardCharsets.UTF_8));
        List<Token> tokens = Tokenizer.tokenize(content);

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.type() == TokenType.VARIABLE_DEF) {
                if (i + 1 < tokens.size()) {
                    Token valueToken = tokens.get(i + 1);
                    if (valueToken.type() == TokenType.VARIABLE_REF) {
                        String resolved = variables.get(valueToken.value());
                        if (resolved != null) {
                            variables.put(token.value(), resolved);
                        }
                    } else {
                        variables.put(token.value(), valueToken.value());
                    }
                    i++;
                }
            }
        }
    }

    static String stripBom(String content) {
        if (!content.isEmpty() && content.charAt(0) == '\uFEFF') {
            return content.substring(1);
        }
        return content;
    }
}
