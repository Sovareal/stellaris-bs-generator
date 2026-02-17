package com.stellaris.bsgenerator.model;

import java.util.List;

/**
 * A trait that can be selected for a starting ruler.
 *
 * @param id               e.g. "leader_trait_principled"
 * @param leaderClasses    which classes can use this trait: "official", "commander", "scientist"
 * @param forbiddenOrigins origins that cannot use this trait
 * @param allowedEthics    if non-empty, only empires with one of these ethics can use it
 * @param allowedOrigins   if non-empty, only empires with one of these origins can use it
 * @param allowedCivics    if non-empty, empire must have at least one of these civics
 * @param forbiddenCivics  if non-empty, empire must NOT have any of these civics
 * @param forbiddenEthics  if non-empty, empire must NOT have any of these ethics
 * @param cost             trait point cost (+1 for positive luminary, -1 for negative, 0 for regular)
 * @param opposites        mutually exclusive trait IDs (negative luminary traits exclude each other)
 */
public record StartingRulerTrait(
        String id,
        List<String> leaderClasses,
        List<String> forbiddenOrigins,
        List<String> allowedEthics,
        List<String> allowedOrigins,
        List<String> allowedCivics,
        List<String> forbiddenCivics,
        List<String> forbiddenEthics,
        int cost,
        List<String> opposites,
        String gfxKey
) {}
