package com.stellaris.bsgenerator.dto;

import com.stellaris.bsgenerator.model.Origin;
import com.stellaris.bsgenerator.parser.LocalizationService;

public record OriginDto(String id, String displayName, String dlcRequirement) {
    public static OriginDto from(Origin o, LocalizationService loc) {
        return new OriginDto(o.id(), loc.getDisplayName(o.id()), o.dlcRequirement());
    }
}
