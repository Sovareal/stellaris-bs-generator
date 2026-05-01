package com.stellaris.bsgenerator.namepool.extractor;

import com.stellaris.bsgenerator.namepool.model.ExtractedNameFile;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * One-off tool: reads Stellaris name_lists and localisation files, builds
 * name_pool_extracted.json in compact JSON format.
 *
 * Run via Gradle: ./gradlew :backend:extractNamePool
 * Or directly: java -cp ... NamePoolExtractorRunner --game-dir=F:/Games/... --output=src/main/resources/data/name_pool_extracted.json
 */
public class NamePoolExtractorRunner {

    private static final Pattern LINE_PATTERN = Pattern.compile(
            "^\\s+(\\S+?)(?::\\d*)?\\s+\"(.*)\"\\s*(?:#.*)?$"
    );
    private static final Pattern FORMAT_CODES = Pattern.compile("§[A-Za-z!_]|£\\w+£|\\[.*?]");

    public static void main(String[] args) throws Exception {
        var params = parseArgs(args);
        Path gameDir    = Path.of(params.getOrDefault("game-dir",
                "F:/Games/SteamLibrary/steamapps/common/Stellaris"));
        Path outputPath = Path.of(params.getOrDefault("output",
                "backend/src/main/resources/data/name_pool_extracted.json"));

        System.out.println("Game dir:  " + gameDir);
        System.out.println("Output:    " + outputPath);

        System.out.println("Loading localisation...");
        var loc = loadLoc(gameDir);
        System.out.println("  Loaded " + loc.size() + " loc keys");

        System.out.println("Extracting ruler names...");
        var rulerResult = new RulerNameExtractor().extract(gameDir.resolve("common"), loc);
        System.out.println("  " + rulerResult.rulerNames().size() + " ruler names, "
                + rulerResult.regnalNames().size() + " regnal names");

        System.out.println("Extracting homeworld names...");
        var homeworldNames = new PlanetSystemNameExtractor().extractHomeworldNames(
                gameDir.resolve("common"), loc);
        System.out.println("  " + homeworldNames.size() + " homeworld names");

        String stellarisVersion = detectVersion(gameDir);

        var extracted = new ExtractedNameFile(
                ExtractedNameFile.CURRENT_SCHEMA_VERSION,
                stellarisVersion,
                Instant.now().toString(),
                rulerResult.rulerNames(),
                rulerResult.regnalNames(),
                homeworldNames
        );

        var mapper = JsonMapper.builder().build();
        Files.createDirectories(outputPath.getParent());
        mapper.writeValue(outputPath.toFile(), extracted);
        System.out.println("Written to " + outputPath);
        System.out.println("Done.");
    }

    private static Map<String, String> loadLoc(Path gameDir) throws IOException {
        Path locDir = gameDir.resolve("localisation").resolve("english");
        var map = new HashMap<String, String>();
        try (Stream<Path> files = Files.walk(locDir)) {
            files.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith("_l_english.yml"))
                    .sorted()
                    .forEach(f -> parseLocFile(f, map));
        }
        return Map.copyOf(map);
    }

    private static void parseLocFile(Path file, Map<String, String> target) {
        try {
            String content = stripBom(Files.readString(file, StandardCharsets.UTF_8));
            for (String line : content.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("l_english")) continue;
                Matcher m = LINE_PATTERN.matcher(line);
                if (m.matches()) {
                    String key   = m.group(1);
                    String value = FORMAT_CODES.matcher(m.group(2)).replaceAll("").replaceAll("\\s+", " ").trim();
                    if (!value.isEmpty()) target.putIfAbsent(key, value);
                }
            }
        } catch (IOException e) {
            System.err.println("  Skipping loc file " + file.getFileName() + ": " + e.getMessage());
        }
    }

    private static String stripBom(String s) {
        return (s.length() > 0 && s.charAt(0) == '﻿') ? s.substring(1) : s;
    }

    private static String detectVersion(Path gameDir) {
        try {
            Path launcher = gameDir.resolve("launcher-settings.json");
            if (Files.exists(launcher)) {
                String content = Files.readString(launcher, StandardCharsets.UTF_8);
                var m = Pattern.compile("\"rawVersion\"\\s*:\\s*\"([^\"]+)\"").matcher(content);
                if (m.find()) return m.group(1);
            }
        } catch (IOException ignored) {}
        return "unknown";
    }

    private static Map<String, String> parseArgs(String[] args) {
        var map = new HashMap<String, String>();
        for (String arg : args) {
            if (arg.startsWith("--")) {
                int eq = arg.indexOf('=');
                if (eq > 0) map.put(arg.substring(2, eq), arg.substring(eq + 1));
            }
        }
        return map;
    }
}
