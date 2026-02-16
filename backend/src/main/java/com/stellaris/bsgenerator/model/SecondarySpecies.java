package com.stellaris.bsgenerator.model;

import java.util.List;

/**
 * A generated secondary species for an empire (e.g., Necrophage prepatent species,
 * Syncretic Evolution servile species, Rogue Servitor bio-trophies).
 *
 * @param title            localization key for the secondary species role
 * @param speciesClass     species class ID (always BIOLOGICAL archetype, different from primary)
 * @param enforcedTraits   traits that cannot be removed (from origin/civic config)
 * @param additionalTraits randomly selected additional traits
 * @param traitPointsUsed  total trait points consumed (enforced + additional)
 * @param traitPointsBudget standard trait point budget (always 2)
 * @param maxTraitPicks    maximum number of trait picks (always 5)
 */
public record SecondarySpecies(
        String title,
        String speciesClass,
        List<SpeciesTrait> enforcedTraits,
        List<SpeciesTrait> additionalTraits,
        int traitPointsUsed,
        int traitPointsBudget,
        int maxTraitPicks
) {}
