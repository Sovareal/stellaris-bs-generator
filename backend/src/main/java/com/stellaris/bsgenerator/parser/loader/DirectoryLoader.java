package com.stellaris.bsgenerator.parser.loader;

import com.stellaris.bsgenerator.parser.ast.ClausewitzNode;
import com.stellaris.bsgenerator.parser.ast.ClausewitzParser;
import com.stellaris.bsgenerator.parser.token.Tokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class DirectoryLoader {

    private static final Logger log = LoggerFactory.getLogger(DirectoryLoader.class);

    private DirectoryLoader() {}

    public static ClausewitzNode loadDirectory(Path directory, Map<String, String> globalVariables) throws IOException {
        if (!Files.isDirectory(directory)) {
            return ClausewitzNode.root(List.of());
        }

        List<Path> files;
        try (Stream<Path> stream = Files.list(directory)) {
            files = stream
                    .filter(p -> p.toString().endsWith(".txt"))
                    .sorted()
                    .toList();
        }

        var allChildren = new ArrayList<ClausewitzNode>();
        for (Path file : files) {
            var fileVars = new HashMap<>(globalVariables);
            ClausewitzNode fileRoot = parseFile(file, fileVars);
            allChildren.addAll(fileRoot.children());
        }

        log.debug("Loaded {} top-level entries from {}", allChildren.size(), directory);
        return ClausewitzNode.root(allChildren);
    }

    static ClausewitzNode parseFile(Path file, Map<String, String> variables) throws IOException {
        String content = ScriptedVariableLoader.stripBom(
                Files.readString(file, StandardCharsets.UTF_8));
        var tokens = Tokenizer.tokenize(content);
        return ClausewitzParser.parse(tokens, variables);
    }
}
