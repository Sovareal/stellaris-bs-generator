package com.stellaris.bsgenerator.engine;

import com.stellaris.bsgenerator.model.*;

import java.util.List;

/**
 * A fully generated empire with all components selected.
 */
public record GeneratedEmpire(
        List<Ethic> ethics,
        Authority authority,
        List<Civic> civics,
        Origin origin,
        SpeciesArchetype speciesArchetype,
        List<SpeciesTrait> speciesTraits,
        int traitPointsUsed,
        int traitPointsBudget
) {}
