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
     * Get species traits compatible with the given archetype.
     * Filters by allowed_archetypes matching the archetype ID.
     */
    public List<SpeciesTrait> getCompatibleTraits(String archetypeId) {
        return gameDataManager.getSpeciesTraits().stream()
                .filter(t -> t.allowedArchetypes().contains(archetypeId))
                .toList();
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
                .filter(a -> !a.id().equals("PRESAPIENT") && !a.id().equals("OTHER"))
                .toList();
    }
}
