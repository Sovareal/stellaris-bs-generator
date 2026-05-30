package com.stellaris.bsgenerator.dto;

import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Set;

public record SettingsResponse(
        String gamePath,
        boolean valid,
        String validationMessage,
        @Nullable Set<String> disabledDlcs,
        List<DlcInfo> availableDlcs
) {}
