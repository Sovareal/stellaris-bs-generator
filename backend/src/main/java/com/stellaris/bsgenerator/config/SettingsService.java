package com.stellaris.bsgenerator.config;

import com.stellaris.bsgenerator.parser.config.ParserProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
            return new Settings(defaultGamePath);
        }
        try {
            return mapper.readValue(settingsFile.toFile(), Settings.class);
        } catch (Exception e) {
            log.warn("Failed to read settings file, using defaults: {}", e.getMessage());
            return new Settings(defaultGamePath);
        }
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
