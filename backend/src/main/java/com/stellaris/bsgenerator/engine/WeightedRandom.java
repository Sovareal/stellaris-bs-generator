package com.stellaris.bsgenerator.engine;

import java.util.List;
import java.util.Random;
import java.util.function.ToIntFunction;

/**
 * Weighted random selection utility.
 */
public final class WeightedRandom {

    private WeightedRandom() {}

    /**
     * Select a random element from a list using weighted probabilities.
     * Elements with weight 0 are excluded. If all weights are 0, falls back to uniform random.
     *
     * @param items the candidates
     * @param weightFn function to extract weight from each item
     * @param random the random source
     * @return a randomly selected item, or null if the list is empty
     */
    public static <T> T select(List<T> items, ToIntFunction<T> weightFn, Random random) {
        if (items.isEmpty()) return null;

        int totalWeight = items.stream().mapToInt(weightFn).sum();

        // Fallback to uniform if all weights are 0
        if (totalWeight <= 0) {
            return items.get(random.nextInt(items.size()));
        }

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (var item : items) {
            cumulative += weightFn.applyAsInt(item);
            if (roll < cumulative) {
                return item;
            }
        }

        // Shouldn't reach here, but safety fallback
        return items.getLast();
    }
}
