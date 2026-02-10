package com.stellaris.bsgenerator.model;

import java.util.List;

public record SpeciesTrait(
        String id,
        int cost,
        List<String> allowedArchetypes,
        List<String> opposites,
        boolean initial,
        boolean randomized,
        String dlcRequirement,
        List<String> tags
) {}
