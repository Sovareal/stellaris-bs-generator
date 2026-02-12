package com.stellaris.bsgenerator.engine;

import com.stellaris.bsgenerator.model.*;
import com.stellaris.bsgenerator.parser.cache.GameDataManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Filters game entities by compatibility with the current empire state.
 * Used during generation to progressively narrow valid choices as
 * each empire component is selected.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompatibilityFilterService {

    private final GameDataManager gameDataManager;
    private final RequirementEvaluator evaluator;

    /**
     * Get authorities compatible with the current empire state.
     * Evaluates both potential and possible blocks.
     */
    public List<Authority> getCompatibleAuthorities(EmpireState state) {
        return gameDataManager.getAuthorities().stream()
                .filter(a -> evaluator.evaluateBoth(a.potential(), a.possible(), state))
                .toList();
    }

    /**
     * Get civics compatible with the current empire state.
     * Filters by potential (visibility), possible (validity), pickable_at_start,
     * and excludes civics already selected in the state.
     */
    public List<Civic> getCompatibleCivics(EmpireState state) {
        return gameDataManager.getCivics().stream()
                .filter(Civic::pickableAtStart)
                .filter(c -> !state.civics().contains(c.id()))
                .filter(c -> evaluator.evaluateBoth(c.potential(), c.possible(), state))
                .toList();
    }

    /**
     * Get origins compatible with the current empire state.
     * Evaluates both potential and possible blocks.
     */
    public List<Origin> getCompatibleOrigins(EmpireState state) {
        return gameDataManager.getOrigins().stream()
                .filter(o -> evaluator.evaluateBoth(o.potential(), o.possible(), state))
                .toList();
    }

    /**
     * Get species traits compatible with the given archetype and empire state.
     * Filters by allowed_archetypes, plus origin/civic/ethic restrictions.
     */
    public List<SpeciesTrait> getCompatibleTraits(String archetypeId, EmpireState state) {
        return gameDataManager.getSpeciesTraits().stream()
                .filter(t -> t.allowedArchetypes().contains(archetypeId))
                .filter(t -> matchesAllowList(t.allowedSpeciesClasses(), state.speciesClass()))
                .filter(t -> matchesAllowList(t.allowedOrigins(), state.origin()))
                .filter(t -> matchesForbidList(t.forbiddenOrigins(), state.origin()))
                .filter(t -> matchesAllowSet(t.allowedCivics(), state.civics()))
                .filter(t -> matchesForbidSet(t.forbiddenCivics(), state.civics()))
                .filter(t -> matchesAllowSet(t.allowedEthics(), state.ethics()))
                .filter(t -> matchesForbidSet(t.forbiddenEthics(), state.ethics()))
                .toList();
    }

    /** If allowList is empty, trait is unrestricted. Otherwise, value must be in list. */
    private boolean matchesAllowList(List<String> allowList, String value) {
        return allowList.isEmpty() || (value != null && allowList.contains(value));
    }

    /** If forbidList is empty, trait is unrestricted. Otherwise, value must NOT be in list. */
    private boolean matchesForbidList(List<String> forbidList, String value) {
        return forbidList.isEmpty() || value == null || !forbidList.contains(value);
    }

    /** If allowList is empty, unrestricted. Otherwise, at least one value must be in the set. */
    private boolean matchesAllowSet(List<String> allowList, java.util.Set<String> values) {
        if (allowList.isEmpty()) return true;
        for (var allowed : allowList) {
            if (values.contains(allowed)) return true;
        }
        return false;
    }

    /** If forbidList is empty, unrestricted. Otherwise, none of the values must be in the set. */
    private boolean matchesForbidSet(List<String> forbidList, java.util.Set<String> values) {
        if (forbidList.isEmpty()) return true;
        for (var forbidden : forbidList) {
            if (values.contains(forbidden)) return false;
        }
        return true;
    }

    /**
     * Get non-gestalt ethics (for regular empire generation).
     */
    public List<Ethic> getRegularEthics() {
        return gameDataManager.getEthics().stream()
                .filter(e -> !e.isGestalt())
                .toList();
    }

    /**
     * Get the gestalt ethic.
     */
    public Ethic getGestaltEthic() {
        return gameDataManager.getEthics().stream()
                .filter(Ethic::isGestalt)
                .findFirst()
                .orElse(null);
    }

    /**
     * Get non-gestalt authorities.
     */
    public List<Authority> getRegularAuthorities() {
        return gameDataManager.getAuthorities().stream()
                .filter(a -> !a.isGestalt())
                .toList();
    }

    /**
     * Get gestalt authorities (hive mind, machine intelligence).
     */
    public List<Authority> getGestaltAuthorities() {
        return gameDataManager.getAuthorities().stream()
                .filter(Authority::isGestalt)
                .toList();
    }

    /**
     * Get player-selectable species archetypes (excludes PRESAPIENT, OTHER).
     */
    public List<SpeciesArchetype> getSelectableArchetypes() {
        return gameDataManager.getSpeciesArchetypes().stream()
                .filter(a -> !a.id().equals("PRESAPIENT") && !a.id().equals("OTHER") && !a.id().equals("ROBOT"))
                .toList();
    }

    /**
     * Get species classes belonging to the given archetype.
     */
    public List<SpeciesClass> getSpeciesClassesForArchetype(String archetypeId) {
        return gameDataManager.getSpeciesClasses().stream()
                .filter(sc -> sc.archetype().equals(archetypeId))
                .toList();
    }

    /**
     * Get all habitable planet classes (initial=yes).
     */
    public List<PlanetClass> getHabitablePlanetClasses() {
        return gameDataManager.getPlanetClasses();
    }

    /**
     * Get all player-selectable graphical cultures (shipsets).
     */
    public List<GraphicalCulture> getSelectableShipsets() {
        return gameDataManager.getGraphicalCultures();
    }

    /**
     * Get starting ruler traits compatible with the given leader class and empire state.
     */
    public List<StartingRulerTrait> getCompatibleRulerTraits(String leaderClass, EmpireState state) {
        return gameDataManager.getStartingRulerTraits().stream()
                .filter(t -> t.leaderClasses().contains(leaderClass))
                .filter(t -> matchesForbidList(t.forbiddenOrigins(), state.origin()))
                .filter(t -> matchesAllowSet(t.allowedEthics(), state.ethics()))
                .filter(t -> matchesAllowList(t.allowedOrigins(), state.origin()))
                .filter(t -> matchesAllowSet(t.allowedCivics(), state.civics()))
                .filter(t -> matchesForbidSet(t.forbiddenCivics(), state.civics()))
                .filter(t -> matchesForbidSet(t.forbiddenEthics(), state.ethics()))
                .toList();
    }
}
