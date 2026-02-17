package com.stellaris.bsgenerator.dto;

import com.stellaris.bsgenerator.model.SpeciesTrait;
import com.stellaris.bsgenerator.parser.LocalizationService;

import java.util.List;

public record TraitDto(String id, String displayName, int cost, List<String> allowedArchetypes, boolean enforced) {
    public static TraitDto from(SpeciesTrait t, LocalizationService loc) {
        return new TraitDto(t.id(), loc.getDisplayName(t.id()), t.cost(), t.allowedArchetypes(), false);
    }

    public static TraitDto fromEnforced(SpeciesTrait t, LocalizationService loc) {
        return new TraitDto(t.id(), loc.getDisplayName(t.id()), t.cost(), t.allowedArchetypes(), true);
    }
}
