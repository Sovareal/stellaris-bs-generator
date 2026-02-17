package com.stellaris.bsgenerator.dto;

import com.stellaris.bsgenerator.model.Ethic;
import com.stellaris.bsgenerator.parser.LocalizationService;

public record EthicDto(String id, String displayName, int cost, boolean isFanatic) {
    public static EthicDto from(Ethic e, LocalizationService loc) {
        return new EthicDto(e.id(), loc.getDisplayName(e.id()), e.cost(), e.isFanatic());
    }
}
