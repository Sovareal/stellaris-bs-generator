package com.stellaris.bsgenerator.config;

import com.stellaris.bsgenerator.parser.config.ParserProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Service
public class SettingsService {

    public record Settings(String gamePath) {}

    private final Path settingsFile;
    private final String defaultGamePath;
    private final JsonMapper mapper;

    public SettingsService(ParserProperties properties) {
        this.settingsFile = Path.of(properties.cachePath()).resolve("settings.json");
        this.defaultGamePath = properties.gamePath();
        this.mapper = JsonMapper.builder().build();
    }

    public Settings load() {
        if (!Files.exists(settingsFile)) {
            return new Settings(resolveGamePath(defaultGamePath));
        }
        try {
            var settings = mapper.readValue(settingsFile.toFile(), Settings.class);
            // If the saved path is blank (e.g. user cleared it), fall back to auto-detect
            if (settings.gamePath() == null || settings.gamePath().isBlank()) {
                return new Settings(resolveGamePath(""));
            }
            return settings;
        } catch (Exception e) {
            log.warn("Failed to read settings file, using defaults: {}", e.getMessage());
            return new Settings(resolveGamePath(defaultGamePath));
        }
    }

    /** Returns {@code configured} if non-blank, otherwise auto-detects the Stellaris install path. */
    private String resolveGamePath(String configured) {
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        String detected = detectDefaultGamePath();
        if (!detected.isBlank()) {
            log.info("Auto-detected Stellaris game path: {}", detected);
        }
        return detected;
    }

    private static String detectDefaultGamePath() {
        String os = System.getProperty("os.name", "").toLowerCase();
        List<Path> candidates;
        if (os.contains("win")) {
            candidates = List.of(
                Path.of("C:/Program Files (x86)/Steam/steamapps/common/Stellaris"),
                Path.of("C:/SteamLibrary/steamapps/common/Stellaris"),
                Path.of("D:/SteamLibrary/steamapps/common/Stellaris"),
                Path.of("E:/SteamLibrary/steamapps/common/Stellaris"),
                Path.of("F:/SteamLibrary/steamapps/common/Stellaris")
            );
        } else if (os.contains("mac")) {
            candidates = List.of(
                Path.of(System.getProperty("user.home"),
                    "Library/Application Support/Steam/steamapps/common/Stellaris")
            );
        } else {
            // Linux (including Steam Deck)
            String home = System.getProperty("user.home", "");
            candidates = List.of(
                Path.of(home, ".steam/steam/steamapps/common/Stellaris"),
                Path.of(home, ".local/share/Steam/steamapps/common/Stellaris"),
                Path.of(home, "snap/steam/common/.steam/steam/steamapps/common/Stellaris")
            );
        }
        return candidates.stream()
                .filter(Files::isDirectory)
                .map(Path::toString)
                .findFirst()
                .orElse("");
    }

    public void save(Settings settings) throws IOException {
        Files.createDirectories(settingsFile.getParent());
        mapper.writeValue(settingsFile.toFile(), settings);
        log.info("Settings saved to {}", settingsFile);
    }

    public String getEffectiveGamePath() {
        return load().gamePath();
    }

    public ValidationResult validate(String gamePath) {
        if (gamePath == null || gamePath.isBlank()) {
            return new ValidationResult(false, "Game path cannot be empty");
        }
        Path path = Path.of(gamePath);
        if (!Files.isDirectory(path)) {
            return new ValidationResult(false, "Directory does not exist: " + gamePath);
        }
        if (!Files.exists(path.resolve("launcher-settings.json"))) {
            return new ValidationResult(false, "Not a Stellaris installation (launcher-settings.json not found)");
        }
        return new ValidationResult(true, "Valid Stellaris installation");
    }

    public record ValidationResult(boolean valid, String message) {}
}
