package com.stellaris.bsgenerator.model.requirement;

import java.util.Map;

/**
 * Categories of requirements found in Clausewitz potential/possible blocks.
 * Each maps from a Clausewitz key to a typed category.
 */
public enum RequirementCategory {
    ETHICS("ethics"),
    AUTHORITY("authority"),
    CIVICS("civics"),
    ORIGIN("origin"),
    TRAITS("traits"),
    SPECIES_CLASS("species_class"),
    SPECIES_ARCHETYPE("species_archetype"),
    GRAPHICAL_CULTURE("graphical_culture"),
    COUNTRY_TYPE("country_type");

    private final String clausewitzKey;

    private static final Map<String, RequirementCategory> BY_KEY;

    static {
        var map = new java.util.HashMap<String, RequirementCategory>();
        for (var cat : values()) {
            map.put(cat.clausewitzKey, cat);
        }
        BY_KEY = Map.copyOf(map);
    }

    RequirementCategory(String clausewitzKey) {
        this.clausewitzKey = clausewitzKey;
    }

    public String clausewitzKey() {
        return clausewitzKey;
    }

    public static RequirementCategory fromKey(String key) {
        return BY_KEY.get(key);
    }
}
