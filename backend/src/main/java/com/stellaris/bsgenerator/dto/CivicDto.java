package com.stellaris.bsgenerator.dto;

import com.stellaris.bsgenerator.model.Civic;
import com.stellaris.bsgenerator.parser.LocalizationService;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public record CivicDto(String id, String displayName, String description, List<ModifierEntry> modifiers) {

    public record ModifierEntry(String name, String value, boolean positive) {}

    public static CivicDto from(Civic c, LocalizationService loc) {
        String description = c.descriptionKey() != null ? loc.getDisplayName(c.descriptionKey()) : null;

        List<ModifierEntry> modifiers = c.modifiers().entrySet().stream()
                .map(e -> toModifierEntry(e.getKey(), e.getValue(), loc))
                .collect(Collectors.toList());

        return new CivicDto(c.id(), loc.getDisplayName(c.id()), description, modifiers);
    }

    private static ModifierEntry toModifierEntry(String key, double value, LocalizationService loc) {
        String name = resolveModifierName(key, loc);
        String formatted = formatModifierValue(key, value);
        return new ModifierEntry(name, formatted, value > 0);
    }

    private static String resolveModifierName(String key, LocalizationService loc) {
        String locKey = "MOD_" + key.toUpperCase(Locale.ROOT);
        String localized = loc.getDisplayName(locKey);
        if (localized != null) return localized;
        return humanizeKey(key);
    }

    private static String formatModifierValue(String key, double value) {
        if (key.endsWith("_mult")) {
            int pct = (int) Math.round(value * 100);
            return (pct > 0 ? "+" : "") + pct + "%";
        }
        if (key.endsWith("_add")) {
            int flat = (int) Math.round(value);
            return (flat > 0 ? "+" : "") + flat;
        }
        // No recognized suffix: use value heuristic
        if (Math.abs(value) < 1.5 && value != Math.floor(value)) {
            int pct = (int) Math.round(value * 100);
            return (pct > 0 ? "+" : "") + pct + "%";
        }
        int flat = (int) Math.round(value);
        return (flat > 0 ? "+" : "") + flat;
    }

    private static String humanizeKey(String key) {
        String spaced = key.replace('_', ' ').trim();
        if (spaced.isEmpty()) return key;
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
}
