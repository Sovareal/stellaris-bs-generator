package com.stellaris.bsgenerator.parser.cache;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record GameVersion(String version, String rawVersion, String buildHash) {

    private static final Pattern BUILD_HASH_PATTERN = Pattern.compile("\\(([a-f0-9]+)\\)");

    public static GameVersion fromLauncherSettings(Path gamePath) throws IOException {
        Path settingsPath = gamePath.resolve("launcher-settings.json");
        if (!Files.exists(settingsPath)) {
            throw new IOException("launcher-settings.json not found at " + settingsPath);
        }

        JsonMapper mapper = JsonMapper.builder().build();
        JsonNode root = mapper.readTree(settingsPath.toFile());

        String version = root.path("version").asText("");
        String rawVersion = root.path("rawVersion").asText("");

        String buildHash = "";
        Matcher m = BUILD_HASH_PATTERN.matcher(version);
        if (m.find()) {
            buildHash = m.group(1);
        }

        return new GameVersion(version, rawVersion, buildHash);
    }

    @Override
    public String toString() {
        return "Stellaris " + rawVersion + " (" + buildHash + ")";
    }
}
