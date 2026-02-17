package com.stellaris.bsgenerator.dto;

import com.stellaris.bsgenerator.model.Authority;
import com.stellaris.bsgenerator.parser.LocalizationService;

public record AuthorityDto(String id, String displayName, boolean isGestalt) {
    public static AuthorityDto from(Authority a, LocalizationService loc) {
        return new AuthorityDto(a.id(), loc.getDisplayName(a.id()), a.isGestalt());
    }
}
