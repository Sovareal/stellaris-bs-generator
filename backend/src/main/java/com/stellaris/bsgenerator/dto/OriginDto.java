package com.stellaris.bsgenerator.dto;

import com.stellaris.bsgenerator.model.Origin;
import com.stellaris.bsgenerator.parser.LocalizationService;
import org.springframework.lang.Nullable;

public record OriginDto(String id, @Nullable String displayName, @Nullable String dlcRequirement) {
    public static OriginDto from(Origin o, LocalizationService loc, boolean nomadic) {
        String nameKey = nomadic && o.nomadicNameKey() != null ? o.nomadicNameKey() : o.id();
        return new OriginDto(o.id(), loc.getDisplayName(nameKey), o.dlcRequirement());
    }
}
