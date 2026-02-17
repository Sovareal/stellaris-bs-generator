package com.stellaris.bsgenerator.model;

import com.stellaris.bsgenerator.model.requirement.RequirementBlock;

import java.util.List;

public record Origin(
        String id,
        RequirementBlock potential,
        RequirementBlock possible,
        String dlcRequirement,
        int randomWeight,
        SecondarySpeciesConfig secondarySpecies,
        List<String> enforcedTraitIds,
        String iconPath
) {}
