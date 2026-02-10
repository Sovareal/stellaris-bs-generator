package com.stellaris.bsgenerator.parser.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GameVersionTest {

    @TempDir
    Path tempDir;

    @Test
    void parsesLauncherSettings() throws IOException {
        Files.writeString(tempDir.resolve("launcher-settings.json"), """
                {
                    "version": "Corvus v4.2.4 (7a03)",
                    "rawVersion": "v4.2.4"
                }
                """);

        GameVersion gv = GameVersion.fromLauncherSettings(tempDir);
        assertEquals("Corvus v4.2.4 (7a03)", gv.version());
        assertEquals("v4.2.4", gv.rawVersion());
        assertEquals("7a03", gv.buildHash());
    }

    @Test
    void extractsBuildHash() throws IOException {
        Files.writeString(tempDir.resolve("launcher-settings.json"), """
                {
                    "version": "Test v1.0.0 (abcd1234)",
                    "rawVersion": "v1.0.0"
                }
                """);

        GameVersion gv = GameVersion.fromLauncherSettings(tempDir);
        assertEquals("abcd1234", gv.buildHash());
    }

    @Test
    void missingFileThrows() {
        assertThrows(IOException.class,
                () -> GameVersion.fromLauncherSettings(tempDir));
    }

    private static final String GAME_PATH = "F:\\Games\\SteamLibrary\\steamapps\\common\\Stellaris";

    static boolean gameFilesExist() {
        return Files.exists(Path.of(GAME_PATH, "launcher-settings.json"));
    }

    @Test
    @EnabledIf("gameFilesExist")
    void realLauncherSettings() throws IOException {
        GameVersion gv = GameVersion.fromLauncherSettings(Path.of(GAME_PATH));
        assertFalse(gv.rawVersion().isEmpty());
        assertFalse(gv.buildHash().isEmpty());
    }
}
