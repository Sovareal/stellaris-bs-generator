package com.stellaris.bsgenerator.namepool.model;

import java.util.List;

public record ExtractedNameFile(
        int schemaVersion,
        String stellarisVersion,
        String generatedAt,
        List<String> rulerNames,
        List<String> regnalNames,
        List<String> homeworldNames
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
}
