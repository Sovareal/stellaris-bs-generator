package com.stellaris.bsgenerator.parser;

import com.stellaris.bsgenerator.parser.config.ParserProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalizationService {

    // Matches: key:0 "value" OR key: "value" (no digit) OR key "value"
    private static final Pattern LINE_PATTERN = Pattern.compile(
            "^\\s+(\\S+?)(?::\\d*)?\\s+\"(.*)\"\\s*(?:#.*)?$"
    );

    // Formatting codes to strip from display text (NOT $variable$ references — those are resolved separately)
    private static final Pattern FORMAT_CODES = Pattern.compile(
            "§[A-Za-z!_]|£\\w+£|\\[.*?]"
    );

    // Pattern to find $variable$ references for resolution
    private static final Pattern VAR_REFERENCE = Pattern.compile("\\$([^$]+)\\$");

    private final ParserProperties properties;

    @Getter
    private Map<String, String> localizations = Map.of();

    public void load() {
        Path locDir = Path.of(properties.gamePath())
                .resolve("localisation")
                .resolve("english");

        if (!Files.isDirectory(locDir)) {
            log.warn("Localization directory not found: {}", locDir);
            return;
        }

        var map = new HashMap<String, String>();
        long start = System.currentTimeMillis();

        try (Stream<Path> files = Files.list(locDir)) {
            files.filter(p -> p.getFileName().toString().endsWith("_l_english.yml"))
                    .sorted()
                    .forEach(file -> parseFile(file, map));
        } catch (IOException e) {
            log.error("Failed to list localization files: {}", e.getMessage());
            return;
        }

        // Second pass: resolve $variable$ references
        resolveVariableReferences(map);

        localizations = Map.copyOf(map);
        long elapsed = System.currentTimeMillis() - start;
        log.info("Loaded {} localization keys in {}ms", localizations.size(), elapsed);
    }

    /**
     * Returns the display name for a game entity key, or null if not found.
     */
    public String getDisplayName(String key) {
        return localizations.get(key);
    }

    private void parseFile(Path file, Map<String, String> target) {
        try {
            String content = stripBom(Files.readString(file, StandardCharsets.UTF_8));
            String[] lines = content.split("\n");

            for (String line : lines) {
                // Skip comments and header
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("l_english")) {
                    continue;
                }

                Matcher m = LINE_PATTERN.matcher(line);
                if (m.matches()) {
                    String key = m.group(1);
                    String value = cleanDisplayText(m.group(2));

                    // Only store non-desc, non-tooltip keys (skip _desc, _tt, _effect, etc.)
                    if (!key.contains("_desc") && !key.contains("_tt") && !key.contains("_EFFECT")
                            && !value.isEmpty()) {
                        target.putIfAbsent(key, value);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Failed to read localization file {}: {}", file.getFileName(), e.getMessage());
        }
    }

    /**
     * Resolve $variable$ references in localization values.
     * E.g., trait_fleeting_lithoid = "$trait_fleeting$" → looks up trait_fleeting → "Fleeting"
     */
    private void resolveVariableReferences(Map<String, String> map) {
        int resolved = 0;
        for (var entry : map.entrySet()) {
            String value = entry.getValue();
            if (!value.contains("$")) continue;

            Matcher m = VAR_REFERENCE.matcher(value);
            StringBuilder sb = new StringBuilder();
            boolean changed = false;
            while (m.find()) {
                String varName = m.group(1);
                String replacement = map.get(varName);
                if (replacement != null) {
                    m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                    changed = true;
                } else {
                    // Unresolvable reference — strip it
                    m.appendReplacement(sb, "");
                    changed = true;
                }
            }
            if (changed) {
                m.appendTail(sb);
                String result = sb.toString().replaceAll("\\s+", " ").trim();
                if (!result.isEmpty()) {
                    entry.setValue(result);
                    resolved++;
                }
            }
        }
        if (resolved > 0) {
            log.info("Resolved {} $variable$ references in localization", resolved);
        }
    }

    private static String cleanDisplayText(String raw) {
        String cleaned = FORMAT_CODES.matcher(raw).replaceAll("");
        // Collapse extra whitespace and trim
        return cleaned.replaceAll("\\s+", " ").trim();
    }

    private static String stripBom(String content) {
        if (content.length() > 0 && content.charAt(0) == '\uFEFF') {
            return content.substring(1);
        }
        return content;
    }
}
