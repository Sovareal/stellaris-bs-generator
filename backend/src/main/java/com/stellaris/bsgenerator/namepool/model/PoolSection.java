package com.stellaris.bsgenerator.namepool.model;

import java.util.ArrayList;
import java.util.List;

public record PoolSection(
        List<String> extracted,
        List<String> custom
) {
    public List<String> all() {
        var combined = new ArrayList<>(extracted);
        combined.addAll(custom);
        return List.copyOf(combined);
    }

    public static PoolSection empty() {
        return new PoolSection(List.of(), List.of());
    }

    public static PoolSection withExtracted(List<String> extracted) {
        return new PoolSection(List.copyOf(extracted), List.of());
    }

    public PoolSection withNewExtracted(List<String> newExtracted) {
        return new PoolSection(List.copyOf(newExtracted), custom);
    }
}
