package com.stellaris.bsgenerator.model;

import java.util.List;

public record Ethic(
        String id,
        int cost,
        String category,
        boolean isFanatic,
        boolean isGestalt,
        String regularVariant,
        String fanaticVariant,
        List<String> tags,
        int randomWeight
) {}
