package com.stellaris.bsgenerator.parser.loader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against game updates introducing syntax the tokenizer/parser cannot handle.
 * {@link DirectoryLoader} swallows per-file parse errors (log.warn + skip), so a new operator or
 * identifier character silently drops a whole file (phase 81: ethics, phase 82: authorities).
 * This test parses every file in every directory the app loads and fails on any parse error.
 */
class GameFilesParseCoverageTest {

    private static final String GAME_PATH = "F:/Games/SteamLibrary/steamapps/common/Stellaris";

    /** Directories under common/ that GameFileService and the name pool extractors load. */
    private static final List<String> LOADED_DIRS = List.of(
            "ethics",
            "governments/authorities",
            "governments/civics",
            "species_archetypes",
            "traits",
            "planet_classes",
            "graphical_culture",
            "species_classes",
            "name_lists");

    static boolean gameFilesExist() {
        return Files.isDirectory(Path.of(GAME_PATH, "common"));
    }

    @Test
    @EnabledIf("gameFilesExist")
    void everyLoadedGameFileParses() throws IOException {
        Path common = Path.of(GAME_PATH, "common");
        Map<String, String> globalVars = ScriptedVariableLoader.loadFromDirectory(
                common.resolve("scripted_variables"));

        var failures = new ArrayList<String>();
        int parsed = 0;
        for (String dir : LOADED_DIRS) {
            Path path = common.resolve(dir);
            assertTrue(Files.isDirectory(path), "Expected game directory is missing: " + dir);
            List<Path> files;
            try (Stream<Path> s = Files.list(path)) {
                files = s.filter(p -> p.toString().endsWith(".txt")).sorted().toList();
            }
            for (Path file : files) {
                try {
                    DirectoryLoader.parseFile(file, new java.util.HashMap<>(globalVars));
                    parsed++;
                } catch (Exception e) {
                    failures.add(dir + "/" + file.getFileName() + ": " + e.getMessage());
                }
            }
        }

        assertTrue(parsed > 50, "Suspiciously few files parsed: " + parsed);
        assertTrue(failures.isEmpty(), failures.size() + " game file(s) failed to parse:\n" + String.join("\n", failures));
    }
}
