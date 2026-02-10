package com.stellaris.bsgenerator.model;

import com.stellaris.bsgenerator.model.requirement.RequirementBlock;

public record Origin(
        String id,
        RequirementBlock potential,
        RequirementBlock possible,
        String dlcRequirement,
        int randomWeight
) {}
