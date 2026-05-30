package com.stellaris.bsgenerator.dto;

import com.stellaris.bsgenerator.model.Authority;
import com.stellaris.bsgenerator.parser.LocalizationService;
import org.springframework.lang.Nullable;

public record AuthorityDto(String id, @Nullable String displayName, boolean isGestalt) {
    public static AuthorityDto from(Authority a, LocalizationService loc) {
        return new AuthorityDto(a.id(), loc.getDisplayName(a.id()), a.isGestalt());
    }
}
