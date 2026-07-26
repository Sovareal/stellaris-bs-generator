package com.stellaris.bsgenerator.engine;

import com.stellaris.bsgenerator.extractor.*;
import com.stellaris.bsgenerator.model.*;
import com.stellaris.bsgenerator.parser.LocalizationService;
import com.stellaris.bsgenerator.parser.cache.GameDataManager;
import com.stellaris.bsgenerator.parser.cache.ParsedDataCache;
import com.stellaris.bsgenerator.parser.config.ParserProperties;
import com.stellaris.bsgenerator.config.SettingsService;
import com.stellaris.bsgenerator.parser.loader.GameFileService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIf("gameFilesExist")
class RerollServiceTest {

    private static final String GAME_PATH = "F:\\Games\\SteamLibrary\\steamapps\\common\\Stellaris";

    @TempDir
    static Path tempDir;

    private static EmpireGeneratorService generator;
    private static RerollService rerollService;
    private static GameDataManager gameDataManager;
    private static RequirementEvaluator evaluator;
    private GenerationSession session;

    static boolean gameFilesExist() {
        return Files.isDirectory(Path.of(GAME_PATH, "common"));
    }

    @BeforeAll
    static void setUpOnce() throws IOException {
        var props = new ParserProperties(GAME_PATH, tempDir.toString());
        var settingsService = new SettingsService(props);
        var gameFileService = new GameFileService(props, settingsService);
        var mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        var cache = new ParsedDataCache(props, mapper);
        gameDataManager = new GameDataManager(settingsService, gameFileService, cache,
                new EthicExtractor(), new AuthorityExtractor(),
                new CivicExtractor(), new OriginExtractor(),
                new SpeciesArchetypeExtractor(), new SpeciesTraitExtractor(),
                new PlanetClassExtractor(), new GraphicalCultureExtractor(),
                new StartingRulerTraitExtractor(), new SpeciesClassExtractor(),
                new LocalizationService(props, settingsService));
        gameDataManager.loadGameData(false);

        evaluator = new RequirementEvaluator();
        var filterService = new CompatibilityFilterService(gameDataManager, evaluator, settingsService);
        generator = new EmpireGeneratorService(filterService, evaluator);
        rerollService = new RerollService(filterService, evaluator, generator);
    }

    @BeforeEach
    void setUp() {
        var empire = generator.generate();
        session = new GenerationSession(empire);
    }

    @Test
    void rerollAuthority() {
        // Some empire configurations lock authority (e.g. civics requiring a specific authority).
        // Retry up to 20 times to find a rerollable empire.
        for (int attempt = 0; attempt < 20; attempt++) {
            var empire = generator.generate();
            session.reset(empire);
            try {
                var updated = rerollService.reroll(session, RerollCategory.AUTHORITY);
                assertNotNull(updated);
                assertFalse(session.canReroll(), "Reroll should be used up");
                assertEquals(2, updated.civics().size(), "Civics should still be 2");
                return;
            } catch (GenerationException e) {
                // This empire had no alternative authority (locked by civics/origin); try next
            }
        }
        fail("Could not find a rerollable-authority empire in 20 attempts");
    }

    @Test
    void rerollCivic1() {
        var originalCivic2 = session.getEmpire().civics().get(1).id();
        var updated = rerollService.reroll(session, RerollCategory.CIVIC1);

        assertNotNull(updated);
        assertEquals(originalCivic2, updated.civics().get(1).id(), "Civic 2 should be unchanged");
        assertFalse(session.canReroll());
    }

    @Test
    void rerollCivic2() {
        var originalCivic1 = session.getEmpire().civics().get(0).id();
        var updated = rerollService.reroll(session, RerollCategory.CIVIC2);

        assertNotNull(updated);
        assertEquals(originalCivic1, updated.civics().get(0).id(), "Civic 1 should be unchanged");
        assertFalse(session.canReroll());
    }

    @Test
    void rerollOrigin() {
        var updated = rerollService.reroll(session, RerollCategory.ORIGIN);

        assertNotNull(updated);
        assertEquals(session.getEmpire().authority().id(), updated.authority().id(), "Authority should be unchanged");
        assertFalse(session.canReroll());
    }

    @Test
    void cannotRerollTwice() {
        rerollService.reroll(session, RerollCategory.ORIGIN);
        // Second reroll of ANY big category should fail
        assertThrows(IllegalStateException.class,
                () -> rerollService.reroll(session, RerollCategory.AUTHORITY));
    }

    @Test
    void cannotRerollDifferentCategoryAfterFirst() {
        rerollService.reroll(session, RerollCategory.CIVIC1);
        // Even a different big category should fail after one reroll
        assertThrows(IllegalStateException.class,
                () -> rerollService.reroll(session, RerollCategory.ETHICS));
    }

    @Test
    void addOneTraitIncreasesTraitCount() {
        int before = session.getEmpire().speciesTraits().size();
        var updated = rerollService.addOneTrait(session);
        int after = updated.speciesTraits().size();
        assertEquals(before + 1, after, "addOneTrait should add exactly one trait");
        // Note: traitPointsUsed may exceed budget (in-debt state is valid; 3-case logic enforces
        // balance >= 0 only on the last pick). No strict budget assertion here.
    }

    @Test
    void addOneTraitIsUnlimited() {
        // Adding a trait should not consume the reroll token
        rerollService.addOneTrait(session);
        assertTrue(session.canReroll(), "Reroll token should still be available after addOneTrait");
    }

    @Test
    void rerollSingleTraitConsumesToken() {
        // Retry with different empires: some (e.g. Evolutionary Predators with Malleable Genes)
        // have origin-enforced costs that consume the full budget, leaving no valid replacement.
        for (int attempt = 0; attempt < 20; attempt++) {
            var empire = generator.generate();
            var testSession = new GenerationSession(empire);
            try {
                rerollService.addOneTrait(testSession);
            } catch (GenerationException e) {
                continue; // No traits available to add — try another empire
            }
            var emp = testSession.getEmpire();
            var enforcedIds = new java.util.HashSet<String>();
            enforcedIds.addAll(emp.origin().enforcedTraitIds());
            emp.civics().forEach(c -> enforcedIds.addAll(c.enforcedTraitIds()));
            var nonEnforced = emp.speciesTraits().stream()
                    .filter(t -> !enforcedIds.contains(t.id()))
                    .findFirst();
            if (nonEnforced.isEmpty()) continue;
            try {
                rerollService.rerollSingleTrait(testSession, nonEnforced.get().id());
                assertFalse(testSession.canReroll(), "Reroll token should be consumed after rerollSingleTrait");
                return; // Test passed
            } catch (GenerationException e) {
                // Budget too restricted for replacement in this empire — try another
            }
        }
        fail("Could not find an empire configuration that allows single-trait reroll in 20 attempts");
    }

    @Test
    void newGenerationResetsReroll() {
        rerollService.reroll(session, RerollCategory.ORIGIN);
        assertFalse(session.canReroll());

        var newEmpire = generator.generate();
        session.reset(newEmpire);

        assertTrue(session.canReroll(), "Reroll should be available after new generation");
    }

    @Test
    void rerollPreservesLockedSelections() {
        var original = session.getEmpire();

        var updated = rerollService.reroll(session, RerollCategory.CIVIC1);

        assertEquals(original.ethics(), updated.ethics(), "Ethics should be unchanged");
        assertEquals(original.authority().id(), updated.authority().id(), "Authority should be unchanged");
        assertEquals(original.civics().get(1).id(), updated.civics().get(1).id(), "Civic 2 should be unchanged");
        assertEquals(original.origin().id(), updated.origin().id(), "Origin should be unchanged");
        assertEquals(original.speciesArchetype().id(), updated.speciesArchetype().id(), "Archetype should be unchanged");
        // Random (non-enforced) traits should be preserved; enforced may differ if new civic enforces different traits
        var originalEnforcedIds = new java.util.HashSet<String>();
        originalEnforcedIds.addAll(original.origin().enforcedTraitIds());
        original.civics().forEach(c -> originalEnforcedIds.addAll(c.enforcedTraitIds()));
        var originalRandoms = original.speciesTraits().stream()
                .filter(t -> !originalEnforcedIds.contains(t.id())).toList();
        var updatedEnforcedIds = new java.util.HashSet<String>();
        updatedEnforcedIds.addAll(updated.origin().enforcedTraitIds());
        updated.civics().forEach(c -> updatedEnforcedIds.addAll(c.enforcedTraitIds()));
        var updatedRandoms = updated.speciesTraits().stream()
                .filter(t -> !updatedEnforcedIds.contains(t.id())).toList();
        assertEquals(originalRandoms, updatedRandoms, "Random traits should be preserved on civic reroll");
    }

    @Test
    void rerollOriginRevalidatesCivics() {
        // Planetscapers (civic_environmental_architects) forbids origin_shattered_ring in its
        // own possible.origin block -- a restriction declared only on the civic side, not the
        // origin side, so it can only be caught by re-checking existing civics after the origin
        // changes (phase 80, issue 2).
        var planetscapers = gameDataManager.getCivics().stream()
                .filter(c -> c.id().equals("civic_environmental_architects"))
                .findFirst().orElseThrow();

        GeneratedEmpire base = null;
        Civic keptCivic = null;
        for (int attempt = 0; attempt < 100 && base == null; attempt++) {
            var e = generator.generate();
            if (e.nomadic() || e.civics().size() < 2) continue;
            var other = e.civics().get(1);
            var checkState = EmpireState.empty()
                    .withEthics(new HashSet<>(e.ethics().stream().map(Ethic::id).toList()))
                    .withAuthority(e.authority().id())
                    .withSpeciesArchetype(e.speciesArchetype().id())
                    .withSpeciesClass(e.speciesClass())
                    .withOrigin(e.origin().id())
                    .withNomadic(false)
                    .withCivics(Set.of(other.id()));
            if (evaluator.evaluateBoth(planetscapers.potential(), planetscapers.possible(), checkState)) {
                base = e;
                keptCivic = other;
            }
        }
        assertNotNull(base, "Could not find a base empire compatible with Planetscapers in 100 attempts");

        var synthetic = new GeneratedEmpire(base.ethics(), base.authority(), List.of(planetscapers, keptCivic),
                base.origin(), base.speciesArchetype(), base.speciesClass(), base.speciesTraits(),
                base.traitPointsUsed(), base.traitPointsBudget(), base.homeworld(), base.habitabilityPreference(),
                base.shipset(), base.leaderClass(), base.leaderTraits(), base.secondarySpecies(),
                false, null);

        boolean landedOnShatteredRing = false;
        for (int attempt = 0; attempt < 150 && !landedOnShatteredRing; attempt++) {
            session.reset(synthetic);
            GeneratedEmpire result;
            try {
                result = rerollService.reroll(session, RerollCategory.ORIGIN);
            } catch (GenerationException e) {
                continue;
            }
            if ("origin_shattered_ring".equals(result.origin().id())) {
                landedOnShatteredRing = true;
                assertFalse(result.civics().stream().anyMatch(c -> c.id().equals("civic_environmental_architects")),
                        "Planetscapers must not survive an origin reroll into Shattered Ring");
            }
        }
        assertTrue(landedOnShatteredRing, "Origin reroll never landed on Shattered Ring in 150 attempts -- test inconclusive");
    }

    @Test
    void addOneTraitExcludesPathogenicGenesConflicts() {
        // trait_pathogenic_genes (enforced by origin_synthetic_fertility) is initial=no, so it's
        // outside the creation-eligible trait pool -- its opposites list (Rapid Breeders, Slow
        // Breeders, Adaptive, Egg Laying, etc.) must still be respected when filling random
        // trait picks (phase 80, issue 1).
        var syntheticFertility = gameDataManager.getOrigins().stream()
                .filter(o -> o.id().equals("origin_synthetic_fertility"))
                .findFirst().orElseThrow();
        var conflictingTraits = Set.of(
                "trait_rapid_breeders", "trait_slow_breeders", "trait_humanoid_existential_iteroparity",
                "trait_humanoid_psychological_infertility", "trait_incubator", "trait_plantoid_budding",
                "trait_lithoid_budding", "trait_nomadic", "trait_sedentary", "trait_nonadaptive",
                "trait_adaptive", "trait_extremely_adaptive", "trait_egg_laying", "trait_nascent_stage");

        GeneratedEmpire base = null;
        for (int attempt = 0; attempt < 100 && base == null; attempt++) {
            var e = generator.generate();
            if (!"BIOLOGICAL".equals(e.speciesArchetype().id())) continue;
            var checkState = EmpireState.empty()
                    .withEthics(new HashSet<>(e.ethics().stream().map(Ethic::id).toList()))
                    .withCivics(new HashSet<>(e.civics().stream().map(Civic::id).toList()))
                    .withSpeciesArchetype(e.speciesArchetype().id())
                    .withNomadic(false);
            if (evaluator.evaluateBoth(syntheticFertility.potential(), syntheticFertility.possible(), checkState)) {
                base = e;
            }
        }
        assertNotNull(base, "Could not find a BIOLOGICAL empire compatible with Synthetic Fertility in 100 attempts");

        var enforcedTraits = generator.buildSpeciesTraits(
                base.speciesArchetype(), EmpireState.empty(), syntheticFertility, base.civics());
        int enforcedCost = enforcedTraits.stream().mapToInt(SpeciesTrait::cost).sum();

        var synthetic = new GeneratedEmpire(base.ethics(), base.authority(), base.civics(), syntheticFertility,
                base.speciesArchetype(), base.speciesClass(), enforcedTraits, enforcedCost,
                base.speciesArchetype().traitPoints(), base.homeworld(), base.habitabilityPreference(),
                base.shipset(), base.leaderClass(), base.leaderTraits(), base.secondarySpecies(),
                false, null);

        session.reset(synthetic);
        while (true) {
            try {
                rerollService.addOneTrait(session);
            } catch (GenerationException e) {
                break; // No picks remaining or no compatible trait left to add
            }
        }

        var finalTraitIds = session.getEmpire().speciesTraits().stream().map(SpeciesTrait::id).toList();
        for (var conflicting : conflictingTraits) {
            assertFalse(finalTraitIds.contains(conflicting),
                    conflicting + " conflicts with Pathogenic Genes and must never be added");
        }
    }
}
