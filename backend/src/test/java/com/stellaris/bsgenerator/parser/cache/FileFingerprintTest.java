package com.stellaris.bsgenerator.parser.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileFingerprintTest {

    @TempDir
    Path tempDir;

    private void createCommonFile(String subdir, String filename, String content) throws IOException {
        Path dir = tempDir.resolve("common").resolve(subdir);
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(filename), content);
    }

    @Test
    void deterministic() throws IOException {
        createCommonFile("ethics", "00_ethics.txt", "content");

        String hash1 = FileFingerprint.compute(tempDir, List.of("ethics"));
        String hash2 = FileFingerprint.compute(tempDir, List.of("ethics"));
        assertEquals(hash1, hash2);
    }

    @Test
    void detectsContentChange() throws IOException {
        createCommonFile("ethics", "00_ethics.txt", "original");
        String hash1 = FileFingerprint.compute(tempDir, List.of("ethics"));

        // Modify file (size changes)
        Path file = tempDir.resolve("common/ethics/00_ethics.txt");
        Files.writeString(file, "modified content that is different");
        String hash2 = FileFingerprint.compute(tempDir, List.of("ethics"));

        assertNotEquals(hash1, hash2);
    }

    @Test
    void sortStability() throws IOException {
        createCommonFile("ethics", "01_b.txt", "b");
        createCommonFile("ethics", "00_a.txt", "a");

        String hash1 = FileFingerprint.compute(tempDir, List.of("ethics"));
        String hash2 = FileFingerprint.compute(tempDir, List.of("ethics"));
        assertEquals(hash1, hash2);
    }

    @Test
    void multipleDirectories() throws IOException {
        createCommonFile("ethics", "00_ethics.txt", "ethics");
        createCommonFile("traits", "00_traits.txt", "traits");

        String hash = FileFingerprint.compute(tempDir, List.of("ethics", "traits"));
        assertNotNull(hash);
        assertFalse(hash.isEmpty());
    }

    @Test
    void emptyDirectory() throws IOException {
        Files.createDirectories(tempDir.resolve("common/ethics"));
        String hash = FileFingerprint.compute(tempDir, List.of("ethics"));
        assertNotNull(hash);
    }
}
