package com.stellaris.bsgenerator.dto;

import com.stellaris.bsgenerator.model.StartingRulerTrait;
import com.stellaris.bsgenerator.parser.LocalizationService;

import java.util.List;

public record LeaderDto(String leaderClass, List<LeaderTraitDto> traits) {
    public static LeaderDto from(String leaderClass, List<StartingRulerTrait> leaderTraits, LocalizationService loc) {
        var traitDtos = leaderTraits.stream()
                .map(t -> new LeaderTraitDto(t.id(), loc.getDisplayName(t.id()), t.cost(), t.gfxKey()))
                .toList();
        return new LeaderDto(leaderClass, traitDtos);
    }
}
