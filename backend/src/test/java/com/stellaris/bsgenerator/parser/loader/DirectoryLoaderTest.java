package com.stellaris.bsgenerator.parser.loader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DirectoryLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void mergesMultipleFiles() throws IOException {
        Files.writeString(tempDir.resolve("00_first.txt"),
                "entity_a = { cost = 1 }\n", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("01_second.txt"),
                "entity_b = { cost = 2 }\n", StandardCharsets.UTF_8);

        var root = DirectoryLoader.loadDirectory(tempDir, new HashMap<>());
        assertEquals(2, root.children().size());
        assertTrue(root.child("entity_a").isPresent());
        assertTrue(root.child("entity_b").isPresent());
    }

    @Test
    void sortOrder() throws IOException {
        Files.writeString(tempDir.resolve("02_later.txt"),
                "later = { val = 2 }\n", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("00_first.txt"),
                "first = { val = 0 }\n", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("01_middle.txt"),
                "middle = { val = 1 }\n", StandardCharsets.UTF_8);

        var root = DirectoryLoader.loadDirectory(tempDir, new HashMap<>());
        assertEquals(3, root.children().size());
        // Files sorted by name, so children should be: first, middle, later
        assertEquals("first", root.children().get(0).key());
        assertEquals("middle", root.children().get(1).key());
        assertEquals("later", root.children().get(2).key());
    }

    @Test
    void variableIsolationBetweenFiles() throws IOException {
        Files.writeString(tempDir.resolve("00_first.txt"),
                "@local_var = 99\nentity_a = { cost = @local_var }\n", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("01_second.txt"),
                "entity_b = { cost = 5 }\n", StandardCharsets.UTF_8);

        var root = DirectoryLoader.loadDirectory(tempDir, new HashMap<>());
        assertEquals("99", root.child("entity_a").orElseThrow().childValue("cost").orElseThrow());
    }

    @Test
    void globalVariablesAvailableInFiles() throws IOException {
        Files.writeString(tempDir.resolve("00_test.txt"),
                "entity = { weight = @global_weight }\n", StandardCharsets.UTF_8);

        var globals = new HashMap<>(Map.of("global_weight", "42"));
        var root = DirectoryLoader.loadDirectory(tempDir, globals);
        assertEquals("42", root.child("entity").orElseThrow().childValue("weight").orElseThrow());
    }

    @Test
    void bomHandling() throws IOException {
        Files.writeString(tempDir.resolve("00_bom.txt"),
                "\uFEFFentity = { val = 1 }\n", StandardCharsets.UTF_8);

        var root = DirectoryLoader.loadDirectory(tempDir, new HashMap<>());
        assertTrue(root.child("entity").isPresent());
    }

    @Test
    void emptyDirectory() throws IOException {
        var root = DirectoryLoader.loadDirectory(tempDir, new HashMap<>());
        assertTrue(root.children().isEmpty());
    }
}
