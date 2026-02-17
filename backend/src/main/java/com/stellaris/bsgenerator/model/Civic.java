package com.stellaris.bsgenerator.model;

import com.stellaris.bsgenerator.model.requirement.RequirementBlock;

import java.util.List;

public record Civic(
        String id,
        RequirementBlock potential,
        RequirementBlock possible,
        boolean pickableAtStart,
        int randomWeight,
        SecondarySpeciesConfig secondarySpecies,
        List<String> enforcedTraitIds
) {}
