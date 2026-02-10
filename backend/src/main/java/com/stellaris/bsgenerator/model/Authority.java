package com.stellaris.bsgenerator.model;

import com.stellaris.bsgenerator.model.requirement.RequirementBlock;

public record Authority(
        String id,
        String electionType,
        boolean hasHeir,
        RequirementBlock potential,
        RequirementBlock possible,
        int randomWeight,
        boolean isGestalt
) {}
