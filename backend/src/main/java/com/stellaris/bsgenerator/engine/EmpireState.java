package com.stellaris.bsgenerator.engine;

import java.util.List;
import java.util.Set;

/**
 * Represents the current (possibly partial) state of an empire being built.
 * Used by the RequirementEvaluator to check if a candidate entity is compatible.
 * <p>
 * Null/empty fields mean "not yet selected" — requirements against unselected
 * categories are treated as satisfied (they'll be validated when that category is picked).
 */
public record EmpireState(
        Set<String> ethics,
        String authority,
        Set<String> civics,
        String origin,
        Set<String> traits,
        String speciesClass,
        String speciesArchetype
) {
    public static EmpireState empty() {
        return new EmpireState(Set.of(), null, Set.of(), null, Set.of(), null, null);
    }

    public EmpireState withEthics(Set<String> ethics) {
        return new EmpireState(ethics, authority, civics, origin, traits, speciesClass, speciesArchetype);
    }

    public EmpireState withAuthority(String authority) {
        return new EmpireState(ethics, authority, civics, origin, traits, speciesClass, speciesArchetype);
    }

    public EmpireState withCivics(Set<String> civics) {
        return new EmpireState(ethics, authority, civics, origin, traits, speciesClass, speciesArchetype);
    }

    public EmpireState withOrigin(String origin) {
        return new EmpireState(ethics, authority, civics, origin, traits, speciesClass, speciesArchetype);
    }

    public EmpireState withTraits(Set<String> traits) {
        return new EmpireState(ethics, authority, civics, origin, traits, speciesClass, speciesArchetype);
    }

    public EmpireState withSpeciesClass(String speciesClass) {
        return new EmpireState(ethics, authority, civics, origin, traits, speciesClass, speciesArchetype);
    }

    public EmpireState withSpeciesArchetype(String speciesArchetype) {
        return new EmpireState(ethics, authority, civics, origin, traits, speciesClass, speciesArchetype);
    }

    /**
     * Get the set of values for a given requirement category in the current state.
     * Returns null if the category has not been selected yet (single-value categories
     * where the value is null), or a set of values for multi-value categories.
     */
    public Set<String> valuesForCategory(com.stellaris.bsgenerator.model.requirement.RequirementCategory category) {
        return switch (category) {
            case ETHICS -> ethics;
            case AUTHORITY -> authority != null ? Set.of(authority) : Set.of();
            case CIVICS -> civics;
            case ORIGIN -> origin != null ? Set.of(origin) : Set.of();
            case TRAITS -> traits;
            case SPECIES_CLASS -> speciesClass != null ? Set.of(speciesClass) : Set.of();
            case SPECIES_ARCHETYPE -> speciesArchetype != null ? Set.of(speciesArchetype) : Set.of();
            case GRAPHICAL_CULTURE -> Set.of(); // Not tracked in empire state
            case COUNTRY_TYPE -> Set.of("default"); // Player empires are country_type "default"
        };
    }

    /**
     * Whether a category has been selected (has at least one value).
     */
    public boolean hasCategory(com.stellaris.bsgenerator.model.requirement.RequirementCategory category) {
        return switch (category) {
            case ETHICS -> !ethics.isEmpty();
            case AUTHORITY -> authority != null;
            case CIVICS -> !civics.isEmpty();
            case ORIGIN -> origin != null;
            case TRAITS -> !traits.isEmpty();
            case SPECIES_CLASS -> speciesClass != null;
            case SPECIES_ARCHETYPE -> speciesArchetype != null;
            case COUNTRY_TYPE -> true; // Always "default" for player empires
            case GRAPHICAL_CULTURE -> false;
        };
    }
}
