package com.stellaris.bsgenerator.parser.loader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ScriptedVariableLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void basicVariableLoad() throws IOException {
        Files.writeString(tempDir.resolve("00_vars.txt"),
                "@my_var = 42\n@other = 1.5\n", StandardCharsets.UTF_8);

        var vars = ScriptedVariableLoader.loadFromDirectory(tempDir);
        assertEquals("42", vars.get("my_var"));
        assertEquals("1.5", vars.get("other"));
    }

    @Test
    void crossReferenceVariables() throws IOException {
        Files.writeString(tempDir.resolve("00_vars.txt"),
                "@base = 10\n@derived = @base\n", StandardCharsets.UTF_8);

        var vars = ScriptedVariableLoader.loadFromDirectory(tempDir);
        assertEquals("10", vars.get("base"));
        assertEquals("10", vars.get("derived"));
    }

    @Test
    void multipleFiles() throws IOException {
        Files.writeString(tempDir.resolve("00_vars.txt"),
                "@first = 1\n", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve("01_vars.txt"),
                "@second = 2\n", StandardCharsets.UTF_8);

        var vars = ScriptedVariableLoader.loadFromDirectory(tempDir);
        assertEquals("1", vars.get("first"));
        assertEquals("2", vars.get("second"));
    }

    @Test
    void emptyDirectory() throws IOException {
        var vars = ScriptedVariableLoader.loadFromDirectory(tempDir);
        assertTrue(vars.isEmpty());
    }

    @Test
    void nonExistentDirectory() throws IOException {
        var vars = ScriptedVariableLoader.loadFromDirectory(tempDir.resolve("nonexistent"));
        assertTrue(vars.isEmpty());
    }

    @Test
    void bomStripped() {
        assertEquals("hello", ScriptedVariableLoader.stripBom("\uFEFFhello"));
        assertEquals("hello", ScriptedVariableLoader.stripBom("hello"));
    }
}
