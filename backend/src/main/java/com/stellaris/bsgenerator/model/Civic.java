package com.stellaris.bsgenerator.model;

import com.stellaris.bsgenerator.model.requirement.RequirementBlock;

import java.util.List;
import java.util.Map;

public record Civic(
        String id,
        RequirementBlock potential,
        RequirementBlock possible,
        boolean pickableAtStart,
        int randomWeight,
        SecondarySpeciesConfig secondarySpecies,
        List<String> enforcedTraitIds,
        List<String> requiredShipsetIds,
        String descriptionKey,
        Map<String, Double> modifiers
) {}
