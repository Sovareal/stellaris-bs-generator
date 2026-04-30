package com.stellaris.bsgenerator.namepool.model;

public record NamePool(
        int schemaVersion,
        String generatedAt,
        String stellarisVersion,
        PoolSection empireNames,
        PoolSection rulerNames,
        PoolSection regnalNames,
        PoolSection homeworldNames,
        PoolSection systemNames
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
}
