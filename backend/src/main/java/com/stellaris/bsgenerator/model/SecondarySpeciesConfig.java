package com.stellaris.bsgenerator.model;

import java.util.List;

/**
 * Configuration for a secondary species parsed from origin/civic game data.
 * Defines the title localization key and any enforced traits.
 *
 * @param title        localization key for the secondary species title
 * @param enforcedTraitIds trait IDs that are automatically applied and cannot be removed
 */
public record SecondarySpeciesConfig(
        String title,
        List<String> enforcedTraitIds
) {}
