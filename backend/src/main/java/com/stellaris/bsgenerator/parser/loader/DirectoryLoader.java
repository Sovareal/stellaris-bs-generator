package com.stellaris.bsgenerator.parser.loader;

import com.stellaris.bsgenerator.parser.ast.ClausewitzNode;
import com.stellaris.bsgenerator.parser.ast.ClausewitzParser;
import com.stellaris.bsgenerator.parser.token.Tokenizer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
public final class DirectoryLoader {

    private DirectoryLoader() {}

    /** Lenient load: unparseable files are logged and skipped. Prefer {@link #loadDirectoryReporting}. */
    public static ClausewitzNode loadDirectory(Path directory, Map<String, String> globalVariables) throws IOException {
        return loadDirectoryReporting(directory, globalVariables).root();
    }

    /** Strict load: throws if any file in the directory cannot be parsed. */
    public static ClausewitzNode loadDirectoryStrict(Path directory, Map<String, String> globalVariables) throws IOException {
        var result = loadDirectoryReporting(directory, globalVariables);
        if (!result.failures().isEmpty()) {
            throw new IOException("Failed to parse game files: " + result.failures());
        }
        return result.root();
    }

    /** Loads every .txt file; files that fail to parse are skipped but reported in the result. */
    public static LoadResult loadDirectoryReporting(Path directory, Map<String, String> globalVariables) throws IOException {
        if (!Files.isDirectory(directory)) {
            return new LoadResult(ClausewitzNode.root(List.of()), List.of());
        }

        List<Path> files;
        try (Stream<Path> stream = Files.list(directory)) {
            files = stream
                    .filter(p -> p.toString().endsWith(".txt"))
                    .sorted()
                    .toList();
        }

        var allChildren = new ArrayList<ClausewitzNode>();
        var failures = new ArrayList<ParseFailure>();
        for (Path file : files) {
            var fileVars = new HashMap<>(globalVariables);
            try {
                ClausewitzNode fileRoot = parseFile(file, fileVars);
                allChildren.addAll(fileRoot.children());
            } catch (Exception e) {
                log.warn("Skipping file {} due to parse error: {}", file.getFileName(), e.getMessage());
                failures.add(new ParseFailure(directory.getFileName().toString(),
                        file.getFileName().toString(), e.getMessage()));
            }
        }

        log.debug("Loaded {} top-level entries from {}", allChildren.size(), directory);
        return new LoadResult(ClausewitzNode.root(allChildren), failures);
    }

    static ClausewitzNode parseFile(Path file, Map<String, String> variables) throws IOException {
        String content = ScriptedVariableLoader.stripBom(
                Files.readString(file, StandardCharsets.UTF_8));
        var tokens = Tokenizer.tokenize(content);
        return ClausewitzParser.parse(tokens, variables);
    }
}
