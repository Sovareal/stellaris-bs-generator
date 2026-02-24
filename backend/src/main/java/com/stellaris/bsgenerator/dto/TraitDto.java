package com.stellaris.bsgenerator.dto;

import com.stellaris.bsgenerator.model.SpeciesTrait;
import com.stellaris.bsgenerator.parser.LocalizationService;

import java.util.List;

public record TraitDto(String id, String displayName, int cost, List<String> allowedArchetypes, boolean enforced, boolean free) {
    /** Normal (random) trait. */
    public static TraitDto from(SpeciesTrait t, LocalizationService loc) {
        return new TraitDto(t.id(), loc.getDisplayName(t.id()), t.cost(), t.allowedArchetypes(), false, false);
    }

    /** Civic-enforced trait: locked and counts toward picks + budget. */
    public static TraitDto fromEnforced(SpeciesTrait t, LocalizationService loc) {
        return new TraitDto(t.id(), loc.getDisplayName(t.id()), t.cost(), t.allowedArchetypes(), true, false);
    }

    /** Origin-enforced trait: locked, does NOT count toward picks or budget (cost is 0 in display). */
    public static TraitDto fromFreeEnforced(SpeciesTrait t, LocalizationService loc) {
        return new TraitDto(t.id(), loc.getDisplayName(t.id()), 0, t.allowedArchetypes(), true, true);
    }
}
