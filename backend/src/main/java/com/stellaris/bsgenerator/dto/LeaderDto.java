package com.stellaris.bsgenerator.dto;

import com.stellaris.bsgenerator.model.StartingRulerTrait;
import com.stellaris.bsgenerator.parser.LocalizationService;

import java.util.List;

public record LeaderDto(String leaderClass, List<LeaderTraitDto> traits,
                        int leaderPicksMax, int leaderBudget) {
    public static LeaderDto from(String leaderClass, List<StartingRulerTrait> leaderTraits,
                                  LocalizationService loc, String originId) {
        var traitDtos = leaderTraits.stream()
                .map(t -> new LeaderTraitDto(t.id(), loc.getDisplayName(t.id()), t.cost(), t.gfxKey()))
                .toList();
        boolean isLuminary = "origin_legendary_leader".equals(originId);
        return new LeaderDto(leaderClass, traitDtos,
                isLuminary ? 3 : 0,
                isLuminary ? 1 : 0);
    }
}
