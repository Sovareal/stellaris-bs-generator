package com.stellaris.bsgenerator.namepool.model;

import java.util.List;

public record CustomNameFile(
        int schemaVersion,
        List<String> empireNames,
        List<String> rulerNames,
        List<String> regnalNames,
        List<String> homeworldNames,
        List<String> systemNames
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
}
