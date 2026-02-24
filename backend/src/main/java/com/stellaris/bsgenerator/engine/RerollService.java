package com.stellaris.bsgenerator.engine;

import com.stellaris.bsgenerator.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Handles rerolling individual categories of a generated empire.
 * Only one reroll is allowed per generation session.
 * The rerolled component must be compatible with all locked (non-rerolled) selections.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RerollService {

    private static final int MAX_REROLL_ATTEMPTS = 50;

    private final CompatibilityFilterService filterService;
    private final RequirementEvaluator evaluator;
    private final EmpireGeneratorService generatorService;

    private final Random random = new Random();

    /**
     * Reroll a specific category, returning an updated empire.
     *
     * @throws GenerationException if no valid replacement can be found
     * @throws IllegalStateException if the reroll has already been used
     */
    public GeneratedEmpire reroll(GenerationSession session, RerollCategory category) {
        if (!session.canReroll()) {
            throw new IllegalStateException("Reroll already used for this generation");
        }

        var empire = session.getEmpire();
        var updated = switch (category) {
            case ETHICS -> rerollEthics(empire);
            case AUTHORITY -> rerollAuthority(empire);
            case CIVIC1 -> rerollCivic(empire, 0);
            case CIVIC2 -> rerollCivic(empire, 1);
            case ORIGIN -> rerollOrigin(empire);
            case HOMEWORLD -> rerollHomeworld(empire);
            case SHIPSET -> rerollShipset(empire);
            case LEADER -> rerollLeader(empire);
            case SECONDARY_SPECIES -> rerollSecondarySpecies(empire);
            case TRAIT_SINGLE, TRAIT_ADD, LEADER_TRAIT_ADD ->
                throw new IllegalArgumentException("Use dedicated methods for " + category);
        };

        session.markRerolled();
        session.setEmpire(updated);

        log.info("Rerolled {}: {}", category, describeChange(empire, updated, category));
        return updated;
    }

    private GeneratedEmpire rerollEthics(GeneratedEmpire empire) {
        for (int attempt = 0; attempt < MAX_REROLL_ATTEMPTS; attempt++) {
            var newEmpire = generatorService.generate();
            var candidateEthics = newEmpire.ethics();

            var state = EmpireState.empty()
                    .withEthics(toEthicIds(candidateEthics))
                    .withSpeciesArchetype(empire.speciesArchetype().id())
                    .withSpeciesClass(empire.speciesClass());
            if (!evaluator.evaluateBoth(empire.authority().potential(), empire.authority().possible(), state)) {
                continue;
            }

            state = state.withAuthority(empire.authority().id());
            boolean civicsCompatible = true;
            for (var civic : empire.civics()) {
                if (!evaluator.evaluateBoth(civic.potential(), civic.possible(), state)) {
                    civicsCompatible = false;
                    break;
                }
            }
            if (!civicsCompatible) continue;

            state = state.withCivics(toCivicIds(empire.civics()));
            if (!evaluator.evaluateBoth(empire.origin().potential(), empire.origin().possible(), state)) {
                continue;
            }

            if (toEthicIds(candidateEthics).equals(toEthicIds(empire.ethics()))) continue;

            return copyWith(empire, b -> b.ethics = candidateEthics);
        }
        // GC empires: no non-GC ethics can coexist with gestalt authorities.
        // Perform a full political regime change: keep species/traits/homeworld, replace political layer.
        if (empire.ethics().stream().anyMatch(Ethic::isGestalt)) {
            return performRegimeChange(empire);
        }
        throw new GenerationException("Could not find compatible ethics for reroll");
    }

    /**
     * Full political regime change for GC empires rerolling ethics.
     * Generates a fresh non-GC empire and returns it (uses reroll slot).
     */
    private GeneratedEmpire performRegimeChange(GeneratedEmpire empire) {
        for (int attempt = 0; attempt < MAX_REROLL_ATTEMPTS; attempt++) {
            var candidate = generatorService.generate();
            if (candidate.ethics().stream().noneMatch(Ethic::isGestalt)) {
                return candidate;
            }
        }
        throw new GenerationException("Could not perform regime change from Gestalt Consciousness");
    }

    private GeneratedEmpire rerollAuthority(GeneratedEmpire empire) {
        var state = EmpireState.empty()
                .withEthics(toEthicIds(empire.ethics()))
                .withSpeciesArchetype(empire.speciesArchetype().id())
                .withSpeciesClass(empire.speciesClass());
        var compatible = filterService.getCompatibleAuthorities(state).stream()
                .filter(a -> !a.id().equals(empire.authority().id()))
                .filter(a -> {
                    var withAuth = state.withAuthority(a.id());
                    for (var civic : empire.civics()) {
                        if (!evaluator.evaluateBoth(civic.potential(), civic.possible(), withAuth)) return false;
                    }
                    var withCivics = withAuth.withCivics(toCivicIds(empire.civics()));
                    return evaluator.evaluateBoth(empire.origin().potential(), empire.origin().possible(), withCivics);
                }).toList();

        if (compatible.isEmpty()) {
            // Gestalt empires: switching hive↔machine requires archetype change.
            // Generate a fresh empire with the other gestalt authority type.
            if (empire.authority().isGestalt()) {
                return performGestaltSwitch(empire);
            }
            throw new GenerationException("No alternative authorities compatible with current empire");
        }

        var newAuth = WeightedRandom.select(compatible, Authority::randomWeight, random);
        return copyWith(empire, b -> b.authority = newAuth);
    }

    /**
     * Switches gestalt type (hive mind ↔ machine intelligence).
     * Since this requires a different species archetype, generates a full fresh empire of the target type.
     */
    private GeneratedEmpire performGestaltSwitch(GeneratedEmpire empire) {
        String targetAuthId = "auth_hive_mind".equals(empire.authority().id())
                ? "auth_machine_intelligence"
                : "auth_hive_mind";
        for (int attempt = 0; attempt < MAX_REROLL_ATTEMPTS; attempt++) {
            var candidate = generatorService.generate();
            if (targetAuthId.equals(candidate.authority().id())) {
                return candidate;
            }
        }
        throw new GenerationException("Could not switch gestalt type to " + targetAuthId);
    }

    private GeneratedEmpire rerollCivic(GeneratedEmpire empire, int index) {
        var otherCivicId = empire.civics().get(1 - index).id();
        var state = EmpireState.empty()
                .withEthics(toEthicIds(empire.ethics()))
                .withAuthority(empire.authority().id())
                .withSpeciesArchetype(empire.speciesArchetype().id())
                .withSpeciesClass(empire.speciesClass())
                .withCivics(Set.of(otherCivicId));

        var compatible = filterService.getCompatibleCivics(state).stream()
                .filter(c -> !c.id().equals(empire.civics().get(index).id()))
                .toList();

        if (compatible.isEmpty()) {
            throw new GenerationException("No alternative civics compatible with current empire");
        }

        var newCivic = WeightedRandom.select(compatible, Civic::randomWeight, random);
        var newCivics = new ArrayList<>(empire.civics());
        newCivics.set(index, newCivic);
        var civicsList = List.copyOf(newCivics);

        // Re-generate secondary species when civics change (may gain or lose secondary species from civic)
        var newSecondary = generatorService.generateSecondarySpecies(empire.origin(), civicsList, empire.speciesClass());

        // Preserve random traits; update enforced layer for new civic
        var newTraits = preserveRandomTraits(empire, empire.origin(), civicsList);
        int newPointsUsed = newTraits.stream().mapToInt(SpeciesTrait::cost).sum();

        // Re-derive homeworld if trait planet constraints changed (e.g., Aquatic added/removed)
        var newPlanetConstraint = generatorService.collectTraitPlanetClasses(newTraits);
        var oldPlanetConstraint = generatorService.collectTraitPlanetClasses(empire.speciesTraits());
        PlanetClass newHomeworld = empire.homeworld();
        PlanetClass newHabPref = empire.habitabilityPreference();
        if (!newPlanetConstraint.equals(oldPlanetConstraint)) {
            newHomeworld = generatorService.pickHomeworld(empire.origin(), newTraits, empire.speciesClass());
            newHabPref = generatorService.pickHabitabilityPreference(empire.origin(), newHomeworld);
        }
        final var finalHomeworld = newHomeworld;
        final var finalHabPref = newHabPref;
        final int finalPointsUsed = newPointsUsed;
        final var finalTraits = newTraits;

        return copyWith(empire, b -> {
            b.civics = civicsList;
            b.secondarySpecies = newSecondary;
            b.speciesTraits = finalTraits;
            b.traitPointsUsed = finalPointsUsed;
            b.homeworld = finalHomeworld;
            b.habitabilityPreference = finalHabPref;
        });
    }

    private GeneratedEmpire rerollOrigin(GeneratedEmpire empire) {
        var state = EmpireState.empty()
                .withEthics(toEthicIds(empire.ethics()))
                .withAuthority(empire.authority().id())
                .withSpeciesArchetype(empire.speciesArchetype().id())
                .withSpeciesClass(empire.speciesClass())
                .withCivics(toCivicIds(empire.civics()));

        var compatible = filterService.getCompatibleOrigins(state).stream()
                .filter(o -> !o.id().equals(empire.origin().id()))
                .toList();

        if (compatible.isEmpty()) {
            throw new GenerationException("No alternative origins compatible with current empire");
        }

        // Use uniform random — matches pickOrigin() (Phase 11.3 fix, now applied to reroll too)
        var newOrigin = compatible.get(random.nextInt(compatible.size()));
        var stateWithOrigin = state.withOrigin(newOrigin.id());

        // Re-generate secondary species when origin changes
        var newSecondary = generatorService.generateSecondarySpecies(newOrigin, empire.civics(), empire.speciesClass());

        // Preserve random traits; update enforced layer for new origin
        var newTraits = preserveRandomTraits(empire, newOrigin, empire.civics());
        int newPointsUsed = newTraits.stream().mapToInt(SpeciesTrait::cost).sum();

        // Regenerate leader traits: origin change may affect valid trait pool (e.g., Treasure Hunters → other)
        var newLeaderTraits = generatorService.pickLeaderTraits(empire.leaderClass(), stateWithOrigin);

        // Regenerate homeworld and hab pref: origin change affects fixed planets (e.g., Void Dwellers → Habitat)
        var newHomeworld = generatorService.pickHomeworld(newOrigin, newTraits, empire.speciesClass());
        var newHabPref = generatorService.pickHabitabilityPreference(newOrigin, newHomeworld);

        return copyWith(empire, b -> {
            b.origin = newOrigin;
            b.secondarySpecies = newSecondary;
            b.speciesTraits = newTraits;
            b.traitPointsUsed = newPointsUsed;
            b.traitPointsBudget = empire.speciesArchetype().traitPoints();
            b.leaderTraits = newLeaderTraits;
            b.homeworld = newHomeworld;
            b.habitabilityPreference = newHabPref;
        });
    }

    private GeneratedEmpire rerollSecondarySpecies(GeneratedEmpire empire) {
        if (empire.secondarySpecies() == null) {
            throw new GenerationException("No secondary species to reroll");
        }
        var newSecondary = generatorService.generateSecondarySpecies(empire.origin(), empire.civics(), empire.speciesClass());
        if (newSecondary == null) {
            throw new GenerationException("Failed to generate secondary species");
        }
        return copyWith(empire, b -> b.secondarySpecies = newSecondary);
    }

    /**
     * Reroll a single non-enforced species trait, replacing it with a compatible alternative.
     * Consumes the session reroll token — only one reroll is allowed per generation.
     *
     * @throws IllegalStateException if the reroll token has already been used
     * @throws GenerationException   if the target trait is enforced or no replacement exists
     */
    public GeneratedEmpire rerollSingleTrait(GenerationSession session, String targetTraitId) {
        if (!session.canReroll()) {
            throw new IllegalStateException("Reroll already used for this generation");
        }

        var empire = session.getEmpire();

        // Enforced traits (from origin + civics) cannot be individually rerolled
        var enforcedIds = new HashSet<>(generatorService.collectEnforcedTraitIds(empire.origin(), empire.civics()));
        if (enforcedIds.contains(targetTraitId)) {
            throw new GenerationException("Cannot reroll an enforced trait: " + targetTraitId);
        }

        var currentTraits = empire.speciesTraits();
        if (currentTraits.stream().noneMatch(t -> t.id().equals(targetTraitId))) {
            throw new GenerationException("Trait not found in empire: " + targetTraitId);
        }

        // Traits that will be kept (everything except the target)
        var remainingTraits = currentTraits.stream()
                .filter(t -> !t.id().equals(targetTraitId))
                .toList();

        // Exclusion set: kept trait IDs + their opposites (symmetric + inverse) + the target itself (prevent no-op)
        var excludedIds = new HashSet<String>();
        excludedIds.add(targetTraitId);
        for (var t : remainingTraits) {
            excludedIds.add(t.id());
            excludedIds.addAll(t.opposites());
            excludedIds.addAll(filterService.getInverseOpposites(t.id()));
        }

        // Remaining budget = total budget minus ALL kept trait costs (origin-enforced count toward budget)
        int budget = empire.traitPointsBudget();
        int spentByKept = remainingTraits.stream().mapToInt(SpeciesTrait::cost).sum();
        int availableBudget = budget - spentByKept;

        var state = EmpireState.empty()
                .withEthics(toEthicIds(empire.ethics()))
                .withAuthority(empire.authority().id())
                .withCivics(toCivicIds(empire.civics()))
                .withOrigin(empire.origin().id())
                .withSpeciesArchetype(empire.speciesArchetype().id())
                .withSpeciesClass(empire.speciesClass());

        var candidates = filterService.getCompatibleTraits(empire.speciesArchetype().id(), state).stream()
                .filter(t -> !excludedIds.contains(t.id()))
                .filter(t -> t.cost() <= availableBudget)
                .toList();

        if (candidates.isEmpty()) {
            throw new GenerationException("No compatible replacement trait found for: " + targetTraitId);
        }

        var replacement = candidates.get(random.nextInt(candidates.size()));

        // Replace the target in-place to preserve trait order
        var newTraits = new ArrayList<>(currentTraits);
        for (int i = 0; i < newTraits.size(); i++) {
            if (newTraits.get(i).id().equals(targetTraitId)) {
                newTraits.set(i, replacement);
                break;
            }
        }
        var newTraitList = List.copyOf(newTraits);

        int newPointsUsed = newTraitList.stream().mapToInt(SpeciesTrait::cost).sum();

        // Re-derive homeworld if trait planet constraints changed (e.g., Aquatic added/removed)
        var newPlanetConstraint = generatorService.collectTraitPlanetClasses(newTraitList);
        var oldPlanetConstraint = generatorService.collectTraitPlanetClasses(currentTraits);
        PlanetClass newHomeworld = empire.homeworld();
        PlanetClass newHabPref = empire.habitabilityPreference();
        if (!newPlanetConstraint.equals(oldPlanetConstraint)) {
            newHomeworld = generatorService.pickHomeworld(empire.origin(), newTraitList, empire.speciesClass());
            newHabPref = generatorService.pickHabitabilityPreference(empire.origin(), newHomeworld);
        }
        final var finalHomeworld = newHomeworld;
        final var finalHabPref = newHabPref;
        final int finalPointsUsed = newPointsUsed;

        var updated = copyWith(empire, b -> {
            b.speciesTraits = newTraitList;
            b.traitPointsUsed = finalPointsUsed;
            b.homeworld = finalHomeworld;
            b.habitabilityPreference = finalHabPref;
        });

        session.markRerolled();
        session.setEmpire(updated);

        log.info("Single-trait reroll: {} → {}", targetTraitId, replacement.id());
        return updated;
    }

    /**
     * Add one random species trait to the empire. Unlimited — does not consume the session reroll token.
     * Respects the archetype trait budget and the opposites of already-picked traits.
     *
     * @throws GenerationException if no picks remain or no compatible trait can be found
     */
    public GeneratedEmpire addOneTrait(GenerationSession session) {
        var empire = session.getEmpire();
        var archetype = empire.speciesArchetype();

        // Compute enforced ID sets to determine picks remaining
        var originEnforcedIds = new HashSet<>(empire.origin().enforcedTraitIds());
        var civicEnforcedIds = new HashSet<String>();
        for (var civic : empire.civics()) {
            civicEnforcedIds.addAll(civic.enforcedTraitIds());
        }
        var allEnforcedIds = new HashSet<String>();
        allEnforcedIds.addAll(originEnforcedIds);
        allEnforcedIds.addAll(civicEnforcedIds);

        long nonFreeEnforcedCount = empire.speciesTraits().stream()
                .filter(t -> allEnforcedIds.contains(t.id()) && t.cost() != 0)
                .count();
        long randomCount = empire.speciesTraits().stream()
                .filter(t -> !allEnforcedIds.contains(t.id()))
                .count();
        int picksRemaining = archetype.maxTraits() - (int) nonFreeEnforcedCount - (int) randomCount;
        if (picksRemaining <= 0) {
            throw new GenerationException("No trait picks remaining");
        }

        var state = EmpireState.empty()
                .withEthics(toEthicIds(empire.ethics()))
                .withAuthority(empire.authority().id())
                .withCivics(toCivicIds(empire.civics()))
                .withOrigin(empire.origin().id())
                .withSpeciesArchetype(archetype.id())
                .withSpeciesClass(empire.speciesClass());

        var available = filterService.getCompatibleTraits(archetype.id(), state);

        // Exclude all current traits (including enforced) and their opposites (symmetric + inverse)
        var excludedIds = new HashSet<String>();
        var excludedByOpposites = new HashSet<String>();
        for (var t : empire.speciesTraits()) {
            excludedIds.add(t.id());
            excludedByOpposites.addAll(t.opposites());
            excludedByOpposites.addAll(filterService.getInverseOpposites(t.id()));
        }

        int budget = archetype.traitPoints();
        int pointsSpent = empire.traitPointsUsed();
        int balance = budget - pointsSpent; // remaining budget; negative = in debt

        var candidates = available.stream()
                .filter(t -> !excludedIds.contains(t.id()))
                .filter(t -> !excludedByOpposites.contains(t.id()))
                .filter(t -> {
                    if (picksRemaining == 1) {
                        // Last pick: must leave balance >= 0 (finish within budget)
                        return t.cost() <= balance;
                    } else if (balance < 0) {
                        // In debt with picks to spare: only negatives can recover balance
                        return t.cost() < 0;
                    } else {
                        // balance >= 0, picks > 1: any trait allowed; temporary debt is fine
                        return true;
                    }
                })
                .toList();

        if (candidates.isEmpty()) {
            throw new GenerationException("No compatible traits available to add");
        }

        var newTrait = candidates.get(random.nextInt(candidates.size()));

        var newTraits = new ArrayList<>(empire.speciesTraits());
        newTraits.add(newTrait);
        var newTraitList = List.copyOf(newTraits);
        int newPointsUsed = pointsSpent + newTrait.cost();

        // Re-derive homeworld if planet constraints changed (e.g., Aquatic added)
        var newPlanetConstraint = generatorService.collectTraitPlanetClasses(newTraitList);
        var oldPlanetConstraint = generatorService.collectTraitPlanetClasses(empire.speciesTraits());
        PlanetClass newHomeworld = empire.homeworld();
        PlanetClass newHabPref = empire.habitabilityPreference();
        if (!newPlanetConstraint.equals(oldPlanetConstraint)) {
            newHomeworld = generatorService.pickHomeworld(empire.origin(), newTraitList, empire.speciesClass());
            newHabPref = generatorService.pickHabitabilityPreference(empire.origin(), newHomeworld);
        }
        final var finalHomeworld = newHomeworld;
        final var finalHabPref = newHabPref;
        final int finalPointsUsed = newPointsUsed;
        final var finalTraits = newTraitList;

        var updated = copyWith(empire, b -> {
            b.speciesTraits = finalTraits;
            b.traitPointsUsed = finalPointsUsed;
            b.homeworld = finalHomeworld;
            b.habitabilityPreference = finalHabPref;
        });

        session.setEmpire(updated);
        log.info("Added species trait: {}", newTrait.id());
        return updated;
    }

    /**
     * Add one leader trait for Under One Rule (Luminary) empires.
     * Unlimited — does not consume the session reroll token.
     *
     * @throws GenerationException if not Luminary, no picks remain, or no compatible trait found
     */
    public GeneratedEmpire addLeaderTrait(GenerationSession session) {
        var empire = session.getEmpire();

        if (!"origin_legendary_leader".equals(empire.origin().id())) {
            throw new GenerationException("Leader trait rolling is only available for Under One Rule (origin_legendary_leader)");
        }

        int picksRemaining = EmpireGeneratorService.LUMINARY_MAX_PICKS - empire.leaderTraits().size();
        if (picksRemaining <= 0) {
            throw new GenerationException("No leader trait picks remaining");
        }

        var state = EmpireState.empty()
                .withEthics(toEthicIds(empire.ethics()))
                .withAuthority(empire.authority().id())
                .withCivics(toCivicIds(empire.civics()))
                .withOrigin(empire.origin().id());

        var compatible = filterService.getCompatibleRulerTraits(empire.leaderClass(), state);

        var excludedIds = new HashSet<String>();
        var excludedByOpposites = new HashSet<String>();
        for (var t : empire.leaderTraits()) {
            excludedIds.add(t.id());
            excludedByOpposites.addAll(t.opposites());
        }

        int pointsSpent = empire.leaderTraits().stream().mapToInt(StartingRulerTrait::cost).sum();
        int balance = EmpireGeneratorService.LUMINARY_BUDGET - pointsSpent;

        var candidates = compatible.stream()
                .filter(t -> !excludedIds.contains(t.id()))
                .filter(t -> !excludedByOpposites.contains(t.id()))
                .filter(t -> {
                    if (picksRemaining == 1) {
                        // Last pick: must leave balance >= 0
                        return t.cost() <= balance;
                    } else if (balance < 0) {
                        // In debt with picks to spare: only negatives can recover balance
                        return t.cost() < 0;
                    } else {
                        // balance >= 0, picks > 1: any trait allowed; temporary debt is fine
                        return true;
                    }
                })
                .toList();

        if (candidates.isEmpty()) {
            throw new GenerationException("No compatible leader traits available to add");
        }

        var newTrait = candidates.get(random.nextInt(candidates.size()));
        var newTraits = new ArrayList<>(empire.leaderTraits());
        newTraits.add(newTrait);

        var updated = copyWith(empire, b -> b.leaderTraits = List.copyOf(newTraits));
        session.setEmpire(updated);
        log.info("Added leader trait: {}", newTrait.id());
        return updated;
    }

    /**
     * Add one random trait to the secondary species. Unlimited — does not consume the session reroll token.
     * Respects the secondary species trait budget and opposites of already-picked traits.
     *
     * @throws GenerationException if no secondary species exists, no picks remain, or no compatible trait found
     */
    public GeneratedEmpire addOneSecondaryTrait(GenerationSession session) {
        var empire = session.getEmpire();
        var secondary = empire.secondarySpecies();

        if (secondary == null) {
            throw new GenerationException("No secondary species to add traits to");
        }

        int picksRemaining = EmpireGeneratorService.SECONDARY_SPECIES_MAX_PICKS
                - secondary.enforcedTraits().size()
                - secondary.additionalTraits().size();
        if (picksRemaining <= 0) {
            throw new GenerationException("No secondary species trait picks remaining");
        }

        // Secondary species is always biological
        var state = EmpireState.empty()
                .withSpeciesArchetype("BIOLOGICAL")
                .withSpeciesClass(secondary.speciesClass());

        var available = filterService.getCompatibleTraits("BIOLOGICAL", state);

        // Exclude all current traits (enforced + additional) and their opposites (symmetric + inverse)
        var excludedIds = new HashSet<String>();
        var excludedByOpposites = new HashSet<String>();
        for (var t : secondary.enforcedTraits()) {
            excludedIds.add(t.id());
            excludedByOpposites.addAll(t.opposites());
            excludedByOpposites.addAll(filterService.getInverseOpposites(t.id()));
        }
        for (var t : secondary.additionalTraits()) {
            excludedIds.add(t.id());
            excludedByOpposites.addAll(t.opposites());
            excludedByOpposites.addAll(filterService.getInverseOpposites(t.id()));
        }

        int pointsSpent = secondary.traitPointsUsed();
        int balance = EmpireGeneratorService.SECONDARY_SPECIES_BUDGET - pointsSpent;

        var candidates = available.stream()
                .filter(t -> !excludedIds.contains(t.id()))
                .filter(t -> !excludedByOpposites.contains(t.id()))
                .filter(t -> {
                    if (picksRemaining == 1) {
                        return t.cost() <= balance;
                    } else if (balance < 0) {
                        return t.cost() < 0;
                    } else {
                        return true;
                    }
                })
                .toList();

        if (candidates.isEmpty()) {
            throw new GenerationException("No compatible secondary species traits available to add");
        }

        var newTrait = candidates.get(random.nextInt(candidates.size()));
        var newAdditional = new ArrayList<>(secondary.additionalTraits());
        newAdditional.add(newTrait);

        int newPointsUsed = pointsSpent + newTrait.cost();

        var newSecondary = new SecondarySpecies(
                secondary.title(),
                secondary.speciesClass(),
                secondary.enforcedTraits(),
                List.copyOf(newAdditional),
                newPointsUsed,
                secondary.traitPointsBudget(),
                secondary.maxTraitPicks()
        );

        var updated = copyWith(empire, b -> b.secondarySpecies = newSecondary);
        session.setEmpire(updated);
        log.info("Added secondary species trait: {}", newTrait.id());
        return updated;
    }

    /**
     * Preserve existing random traits through an origin or civic change.
     * Gets the new enforced traits for the updated origin/civics, then filters the existing
     * random traits to remove any that conflict with the new enforced trait opposites or are
     * now covered by the new enforced set. Returns enforced + compatible randoms combined.
     */
    private List<SpeciesTrait> preserveRandomTraits(GeneratedEmpire empire, Origin newOrigin, List<Civic> newCivics) {
        // Old enforced IDs to identify which current traits are "random"
        var oldEnforcedIds = new HashSet<>(generatorService.collectEnforcedTraitIds(empire.origin(), empire.civics()));

        // Extract current random traits (not in old enforced set)
        var randomTraits = empire.speciesTraits().stream()
                .filter(t -> !oldEnforcedIds.contains(t.id()))
                .toList();

        // Get new enforced traits (enforced-only, no randoms)
        var stateForBuild = EmpireState.empty()
                .withEthics(toEthicIds(empire.ethics()))
                .withAuthority(empire.authority().id())
                .withSpeciesArchetype(empire.speciesArchetype().id())
                .withSpeciesClass(empire.speciesClass());
        var newEnforcedTraits = generatorService.buildSpeciesTraits(
                empire.speciesArchetype(), stateForBuild, newOrigin, newCivics);

        // Build exclusion sets from new enforced traits
        var newEnforcedIds = new HashSet<String>();
        var newEnforcedOpposites = new HashSet<String>();
        for (var t : newEnforcedTraits) {
            newEnforcedIds.add(t.id());
            newEnforcedOpposites.addAll(t.opposites());
        }

        // Filter randoms: drop those now covered by new enforced or conflicting with their opposites
        var filteredRandoms = randomTraits.stream()
                .filter(t -> !newEnforcedIds.contains(t.id()))
                .filter(t -> !newEnforcedOpposites.contains(t.id()))
                .toList();

        var combined = new ArrayList<>(newEnforcedTraits);
        combined.addAll(filteredRandoms);
        return List.copyOf(combined);
    }

    /**
     * Randomly remove one non-enforced species trait. Used when a civic/origin change pushes the
     * trait count over the archetype maximum. Does NOT consume the session reroll token.
     *
     * @throws GenerationException if there are no removable (non-enforced) traits
     */
    public GeneratedEmpire removeRandomTrait(GenerationSession session) {
        var empire = session.getEmpire();

        // Enforced traits (origin + civic) cannot be removed
        var enforcedIds = new HashSet<>(generatorService.collectEnforcedTraitIds(empire.origin(), empire.civics()));

        var removable = empire.speciesTraits().stream()
                .filter(t -> !enforcedIds.contains(t.id()))
                .toList();

        if (removable.isEmpty()) {
            throw new GenerationException("No removable traits available");
        }

        var toRemove = removable.get(random.nextInt(removable.size()));

        var newTraits = empire.speciesTraits().stream()
                .filter(t -> !t.id().equals(toRemove.id()))
                .toList();

        int newPointsUsed = List.copyOf(newTraits).stream().mapToInt(SpeciesTrait::cost).sum();

        // Re-derive homeworld if planet constraints changed (e.g., Aquatic removed)
        var newPlanetConstraint = generatorService.collectTraitPlanetClasses(List.copyOf(newTraits));
        var oldPlanetConstraint = generatorService.collectTraitPlanetClasses(empire.speciesTraits());
        PlanetClass newHomeworld = empire.homeworld();
        PlanetClass newHabPref = empire.habitabilityPreference();
        if (!newPlanetConstraint.equals(oldPlanetConstraint)) {
            newHomeworld = generatorService.pickHomeworld(empire.origin(), List.copyOf(newTraits), empire.speciesClass());
            newHabPref = generatorService.pickHabitabilityPreference(empire.origin(), newHomeworld);
        }
        final var finalHomeworld = newHomeworld;
        final var finalHabPref = newHabPref;
        final int finalPointsUsed = newPointsUsed;
        final var finalTraits = List.copyOf(newTraits);

        var updated = copyWith(empire, b -> {
            b.speciesTraits = finalTraits;
            b.traitPointsUsed = finalPointsUsed;
            b.homeworld = finalHomeworld;
            b.habitabilityPreference = finalHabPref;
        });

        session.setEmpire(updated);
        log.info("Removed species trait: {}", toRemove.id());
        return updated;
    }

    private GeneratedEmpire rerollTraits(GeneratedEmpire empire) {
        var archetype = empire.speciesArchetype();
        var state = EmpireState.empty()
                .withEthics(toEthicIds(empire.ethics()))
                .withAuthority(empire.authority().id())
                .withCivics(toCivicIds(empire.civics()))
                .withOrigin(empire.origin().id())
                .withSpeciesArchetype(archetype.id())
                .withSpeciesClass(empire.speciesClass());
        var available = filterService.getCompatibleTraits(archetype.id(), state);
        int budget = archetype.traitPoints();

        // Separate origin-free from civic-counted enforced traits
        var originEnforcedIds = new HashSet<>(empire.origin().enforcedTraitIds());
        var civicEnforcedIds = new HashSet<String>();
        for (var civic : empire.civics()) {
            civicEnforcedIds.addAll(civic.enforcedTraitIds());
        }
        var allEnforcedIds = new HashSet<String>();
        allEnforcedIds.addAll(originEnforcedIds);
        allEnforcedIds.addAll(civicEnforcedIds);

        List<SpeciesTrait> enforced = empire.speciesTraits().stream()
                .filter(t -> allEnforcedIds.contains(t.id()))
                .toList();

        // Only civic-enforced traits reduce available random slots
        int maxRandomPicks = archetype.maxTraits() - civicEnforcedIds.size();
        // Only civic-enforced costs count against budget
        int civicEnforcedCostSum = enforced.stream()
                .filter(t -> civicEnforcedIds.contains(t.id()))
                .mapToInt(SpeciesTrait::cost).sum();

        Set<String> pickedIds = new HashSet<>(allEnforcedIds);
        Set<String> excludedByOpposites = new HashSet<>();
        for (var t : enforced) {
            excludedByOpposites.addAll(t.opposites());
        }
        int pointsSpent = civicEnforcedCostSum;
        int randomPicksCount = 0;

        List<SpeciesTrait> randomPicked = new ArrayList<>();
        var shuffled = new ArrayList<>(available);
        Collections.shuffle(shuffled, random);

        for (var trait : shuffled) {
            if (randomPicksCount >= maxRandomPicks) break;
            if (pickedIds.contains(trait.id())) continue;
            if (excludedByOpposites.contains(trait.id())) continue;

            int newTotal = pointsSpent + trait.cost();
            if (newTotal > budget) continue;
            if (newTotal < 0) continue;

            randomPicked.add(trait);
            pickedIds.add(trait.id());
            pointsSpent = newTotal;
            randomPicksCount++;
            excludedByOpposites.addAll(trait.opposites());
        }

        // Enforced first, then random
        var combined = new ArrayList<>(enforced);
        combined.addAll(randomPicked);
        // traitPointsUsed = civic-enforced + random (not origin-enforced)
        int finalPointsSpent = pointsSpent;
        var newTraitList = List.copyOf(combined);

        // Re-derive homeworld if trait planet constraints changed (e.g., Aquatic added/removed)
        var newPlanetConstraint = generatorService.collectTraitPlanetClasses(newTraitList);
        var oldPlanetConstraint = generatorService.collectTraitPlanetClasses(empire.speciesTraits());
        PlanetClass newHomeworld = empire.homeworld();
        PlanetClass newHabPref = empire.habitabilityPreference();
        if (!newPlanetConstraint.equals(oldPlanetConstraint)) {
            newHomeworld = generatorService.pickHomeworld(empire.origin(), newTraitList, empire.speciesClass());
            newHabPref = generatorService.pickHabitabilityPreference(empire.origin(), newHomeworld);
        }
        final var finalHomeworld = newHomeworld;
        final var finalHabPref = newHabPref;

        return copyWith(empire, b -> {
            b.speciesTraits = newTraitList;
            b.traitPointsUsed = finalPointsSpent;
            b.traitPointsBudget = budget;
            b.homeworld = finalHomeworld;
            b.habitabilityPreference = finalHabPref;
        });
    }

    /** Cold planet types that Infernal species cannot inhabit. */
    private static final Set<String> INF_REMOVED_PLANETS = Set.of("pc_arctic", "pc_alpine", "pc_tundra");

    private GeneratedEmpire rerollHomeworld(GeneratedEmpire empire) {
        var planets = new ArrayList<>(filterService.getHabitablePlanetClasses().stream()
                .filter(p -> !p.id().equals(empire.homeworld().id()))
                .toList());

        // Infernal species: add volcanic, remove cold worlds
        if ("INF".equals(empire.speciesClass())) {
            if (planets.stream().noneMatch(p -> "pc_volcanic".equals(p.id()))) {
                planets.add(new PlanetClass("pc_volcanic", "volcanic"));
            }
            planets.removeIf(p -> INF_REMOVED_PLANETS.contains(p.id()));
        }

        // Constrain by trait allowed_planet_classes (e.g., Aquatic → pc_ocean only)
        Set<String> traitPlanetRestriction = generatorService.collectTraitPlanetClasses(empire.speciesTraits());
        if (!traitPlanetRestriction.isEmpty()) {
            planets = new ArrayList<>(planets.stream()
                    .filter(p -> traitPlanetRestriction.contains(p.id()))
                    .toList());
        }

        if (planets.isEmpty()) {
            throw new GenerationException("No alternative homeworld planets available");
        }

        var newPlanet = planets.get(random.nextInt(planets.size()));
        // Hab pref follows homeworld unless origin fixes it
        var newHabPref = empire.origin().habitabilityPreference() != null
                ? empire.habitabilityPreference()
                : newPlanet;
        return copyWith(empire, b -> {
            b.homeworld = newPlanet;
            b.habitabilityPreference = newHabPref;
        });
    }

    private GeneratedEmpire rerollShipset(GeneratedEmpire empire) {
        var shipsets = filterService.getSelectableShipsets().stream()
                .filter(s -> !s.id().equals(empire.shipset().id()))
                .toList();

        if (shipsets.isEmpty()) {
            throw new GenerationException("No alternative shipsets available");
        }

        var newShipset = shipsets.get(random.nextInt(shipsets.size()));
        return copyWith(empire, b -> b.shipset = newShipset);
    }

    private GeneratedEmpire rerollLeader(GeneratedEmpire empire) {
        var state = EmpireState.empty()
                .withEthics(toEthicIds(empire.ethics()))
                .withAuthority(empire.authority().id())
                .withCivics(toCivicIds(empire.civics()))
                .withOrigin(empire.origin().id());

        // Pick a potentially different leader class
        var leaderClasses = List.of("official", "commander", "scientist");
        var newClass = leaderClasses.get(random.nextInt(leaderClasses.size()));

        List<StartingRulerTrait> newTraits = generatorService.pickLeaderTraits(newClass, state);
        return copyWith(empire, b -> {
            b.leaderClass = newClass;
            b.leaderTraits = newTraits;
        });
    }

    // --- Helper to build empire copies with selective changes ---

    private static class EmpireBuilder {
        List<Ethic> ethics;
        Authority authority;
        List<Civic> civics;
        Origin origin;
        SpeciesArchetype speciesArchetype;
        String speciesClass;
        List<SpeciesTrait> speciesTraits;
        int traitPointsUsed;
        int traitPointsBudget;
        PlanetClass homeworld;
        PlanetClass habitabilityPreference;
        GraphicalCulture shipset;
        String leaderClass;
        List<StartingRulerTrait> leaderTraits;
        SecondarySpecies secondarySpecies;

        EmpireBuilder(GeneratedEmpire e) {
            this.ethics = e.ethics();
            this.authority = e.authority();
            this.civics = e.civics();
            this.origin = e.origin();
            this.speciesArchetype = e.speciesArchetype();
            this.speciesClass = e.speciesClass();
            this.speciesTraits = e.speciesTraits();
            this.traitPointsUsed = e.traitPointsUsed();
            this.traitPointsBudget = e.traitPointsBudget();
            this.homeworld = e.homeworld();
            this.habitabilityPreference = e.habitabilityPreference();
            this.shipset = e.shipset();
            this.leaderClass = e.leaderClass();
            this.leaderTraits = e.leaderTraits();
            this.secondarySpecies = e.secondarySpecies();
        }

        GeneratedEmpire build() {
            return new GeneratedEmpire(ethics, authority, civics, origin,
                    speciesArchetype, speciesClass, speciesTraits, traitPointsUsed, traitPointsBudget,
                    homeworld, habitabilityPreference, shipset, leaderClass, leaderTraits, secondarySpecies);
        }
    }

    private GeneratedEmpire copyWith(GeneratedEmpire empire, java.util.function.Consumer<EmpireBuilder> mutator) {
        var builder = new EmpireBuilder(empire);
        mutator.accept(builder);
        return builder.build();
    }

    // --- ID extraction helpers ---

    private Set<String> toEthicIds(List<Ethic> ethics) {
        var set = new HashSet<String>();
        for (var e : ethics) set.add(e.id());
        return set;
    }

    private Set<String> toCivicIds(List<Civic> civics) {
        var set = new HashSet<String>();
        for (var c : civics) set.add(c.id());
        return set;
    }

    private String describeChange(GeneratedEmpire old, GeneratedEmpire updated, RerollCategory cat) {
        return switch (cat) {
            case ETHICS -> old.ethics().stream().map(Ethic::id).toList() + " → " + updated.ethics().stream().map(Ethic::id).toList();
            case AUTHORITY -> old.authority().id() + " → " + updated.authority().id();
            case CIVIC1 -> old.civics().get(0).id() + " → " + updated.civics().get(0).id();
            case CIVIC2 -> old.civics().get(1).id() + " → " + updated.civics().get(1).id();
            case ORIGIN -> old.origin().id() + " → " + updated.origin().id();
            case HOMEWORLD -> old.homeworld().id() + " → " + updated.homeworld().id();
            case SHIPSET -> old.shipset().id() + " → " + updated.shipset().id();
            case LEADER -> (old.leaderClass() + "/" + old.leaderTraits().stream().map(StartingRulerTrait::id).toList())
                    + " → " + (updated.leaderClass() + "/" + updated.leaderTraits().stream().map(StartingRulerTrait::id).toList());
            case SECONDARY_SPECIES -> (old.secondarySpecies() != null ? old.secondarySpecies().speciesClass() : "none")
                    + " → " + (updated.secondarySpecies() != null ? updated.secondarySpecies().speciesClass() : "none");
            case TRAIT_SINGLE -> old.speciesTraits().stream().map(SpeciesTrait::id).toList()
                    + " → " + updated.speciesTraits().stream().map(SpeciesTrait::id).toList();
            case TRAIT_ADD -> "+ " + updated.speciesTraits().getLast().id();
            case LEADER_TRAIT_ADD -> "+ " + updated.leaderTraits().getLast().id();
        };
    }
}
