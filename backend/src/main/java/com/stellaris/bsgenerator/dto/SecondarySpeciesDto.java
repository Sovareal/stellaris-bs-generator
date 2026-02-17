package com.stellaris.bsgenerator.dto;

import com.stellaris.bsgenerator.model.SecondarySpecies;
import com.stellaris.bsgenerator.parser.LocalizationService;

import java.util.List;

public record SecondarySpeciesDto(
        String title,
        String titleDisplayName,
        String speciesClass,
        String speciesClassName,
        List<TraitDto> enforcedTraits,
        List<TraitDto> additionalTraits,
        int traitPointsUsed,
        int traitPointsBudget,
        int maxTraitPicks
) {
    public static SecondarySpeciesDto from(SecondarySpecies ss, LocalizationService loc) {
        if (ss == null) return null;
        return new SecondarySpeciesDto(
                ss.title(),
                loc.getDisplayName(ss.title()),
                ss.speciesClass(),
                loc.getDisplayName(ss.speciesClass()),
                ss.enforcedTraits().stream().map(t -> TraitDto.from(t, loc)).toList(),
                ss.additionalTraits().stream().map(t -> TraitDto.from(t, loc)).toList(),
                ss.traitPointsUsed(),
                ss.traitPointsBudget(),
                ss.maxTraitPicks()
        );
    }
}
