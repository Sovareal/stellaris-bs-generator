package com.stellaris.bsgenerator.model;

import java.util.List;

public record SpeciesTrait(
        String id,
        int cost,
        List<String> allowedArchetypes,
        List<String> allowedSpeciesClasses,
        List<String> allowedPlanetClasses,
        List<String> opposites,
        boolean initial,
        boolean randomized,
        String dlcRequirement,
        List<String> tags,
        List<String> allowedOrigins,
        List<String> forbiddenOrigins,
        List<String> allowedCivics,
        List<String> forbiddenCivics,
        List<String> allowedEthics,
        List<String> forbiddenEthics
) {}
