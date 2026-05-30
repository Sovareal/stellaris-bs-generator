package com.stellaris.bsgenerator.dto;

import org.springframework.lang.Nullable;

public record LeaderTraitDto(String id, @Nullable String displayName, int cost, @Nullable String gfxKey) {}
