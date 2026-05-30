package com.stellaris.bsgenerator.dto;

import com.stellaris.bsgenerator.model.SpeciesArchetype;
import com.stellaris.bsgenerator.parser.LocalizationService;
import org.springframework.lang.Nullable;

public record ArchetypeDto(String id, @Nullable String displayName, int traitPoints, int maxTraits, boolean robotic) {
    public static ArchetypeDto from(SpeciesArchetype a, LocalizationService loc) {
        return new ArchetypeDto(a.id(), loc.getDisplayName(a.id()), a.traitPoints(), a.maxTraits(), a.robotic());
    }
}
