# Stellaris BS Empire Generator - Progress Tracker

> **How to use:** Update status and notes as each task is completed. This file is the
> primary context anchor across sessions — read it first to understand where we left off.

## Current Session Focus
Phase 10 in progress. Tasks 10.1–10.3 (localization fixes, homeworld constraint, display fix) complete. Task 10.4 (multi-species origins) and 10.5 (error handling) remaining.

## Last Completed Task
Task 10.3 — Trait points display fix (picks/max + pts remaining)

## Next Up
Task 10.4: Multi-species origins (Necrophage, Syncretic Evolution, Rogue Servitor, Driven Assimilator)

---

## Phase 0: Project Scaffolding

| Task | Description | Status | Notes |
|------|-------------|--------|-------|
| 0.0 | Initialize Git Repository | DONE | .gitignore, README.md, initial commit, all scaffolding committed |
| 0.1 | Initialize Gradle Backend Project | DONE | Spring Boot 4.0.2, Java 25, Gradle. Builds and runs on :8080 |
| 0.2 | Initialize Tauri + React Frontend | DONE | Vite+React+Tailwind+Tauri 2.0 initialized. Tauri Rust build blocked on Windows SDK (kernel32.lib missing) |
| 0.3 | Sidecar Wiring | DONE | Rust sidecar launcher in lib.rs, frontend useBackendReady hook. Needs runtime verification after Windows SDK install |

## Phase 1: Clausewitz File Parser (Backend Core)

| Task | Description | Status | Notes |
|------|-------------|--------|-------|
| 1.1 | Clausewitz Tokenizer | DONE | Single-pass char lexer. Handles \|, /, ' chars found in real game files. 18 test cases. |
| 1.2 | Clausewitz AST Parser | DONE | Recursive descent → ClausewitzNode tree. Variable resolution, bare values, repeated keys. 14 tests. |
| 1.3 | Multi-File Loader | DONE | ScriptedVariableLoader + DirectoryLoader + GameFileService. BOM stripping, file-local var isolation. Integration test with real game files. |
| 1.4 | Game Version Detection & Data Cache | DONE | GameVersion, FileFingerprint (SHA-256), ParsedDataCache (Jackson 3), GameDataManager, DataController. Endpoints: GET /api/data/version, POST /api/data/reload. |

## Phase 2: Game Data Models & Extraction

| Task | Description | Status | Notes |
|------|-------------|--------|-------|
| 2.1 | Ethics Model & Extractor | DONE | 17 ethics (8 regular + 8 fanatic + 1 gestalt). Derives isFanatic from cost, links regular/fanatic variants. |
| 2.2 | Authority Model & Extractor | DONE | 7 player authorities. Filters AI-only (auth_ancient_machine_intelligence). Parses potential/possible via RequirementBlockParser. |
| 2.3 | Civic Model & Extractor | DONE | 265 civics. Excludes origins (is_origin=yes). Parses potential/possible blocks. |
| 2.4 | Origin Model & Extractor | DONE | 58 playable origins. Filters non-playable (always=no). Extracts DLC requirements from playable block. |
| 2.5 | Species Archetype & Trait Models | DONE | 6 archetypes (resolves inherit_trait_points_from). 154 creation-eligible traits (filters auto_mod, initial=no). |

## Phase 3: Rule / Constraint Engine

| Task | Description | Status | Notes |
|------|-------------|--------|-------|
| 3.1 | Requirement Block Data Model | DONE | Pulled forward into Phase 2 — sealed Requirement interface, RequirementCategory enum, RequirementBlock record, RequirementBlockParser. |
| 3.2 | Requirement Evaluator | DONE | EmpireState record + RequirementEvaluator. Pattern matching on sealed Requirement. Defers unselected categories. 12 unit tests. |
| 3.3 | Compatibility Filter Service | DONE | CompatibilityFilterService: filters authorities/civics/origins/traits by compatibility. Excludes already-selected civics. 13 integration tests with full chain. |

## Phase 4: Empire Generation Engine

| Task | Description | Status | Notes |
|------|-------------|--------|-------|
| 4.1 | Random Empire Generator Service | DONE | EmpireState, WeightedRandom, EmpireGeneratorService. Ethics axis detection via category field. 100-rep stress test passes. |
| 4.2 | Session State & Reroll Logic | DONE | GenerationSession, RerollCategory, RerollService. Per-category reroll with locked selection preservation. 9 integration tests. |
| 4.3 | REST API Endpoints | DONE | EmpireController (POST /api/empire/generate, /reroll). GlobalExceptionHandler. DTO records with reroll availability map. |

## Phase 5: Frontend UI

| Task | Description | Status | Notes |
|------|-------------|--------|-------|
| 5.1 | App Layout & Theme | DONE | shadcn/ui (button/card/badge/tooltip), Stellaris dark theme, Header/Footer/LoadingScreen/ErrorScreen. Path aliases (@/). |
| 5.2 | Backend Connection Hook | DONE | useBackendReady hook — now also fetches game version from /api/data/version |
| 5.3 | Empire Display Card | DONE | EmpireView, EmpireCard, EmpireSlot, EthicsSlot (fanatic badges), TraitsSlot (cost coloring, point budget) |
| 5.4 | Generate & Reroll Controls | DONE | GenerateButton (sparkles/spinner), RerollButton (dice/tooltip/disabled states), ErrorToast (dismissible) |
| 5.5 | Session State (Zustand) | DONE | TypeScript types matching backend DTOs, typed API client with ApiError, Zustand store with generate/reroll/generationId |

## Phase 6: Generation Fixes (Post-Playtest)

| Task | Description | Status | Notes |
|------|-------------|--------|-------|
| 6.1 | Gestalt Empire Generation | DONE | ~15% gestalt branch in pickEthics(). 200-rep stress test validates gestalt gets gestalt authorities, correct archetypes. |
| 6.2 | Origin-Aware Trait Filtering | DONE | Added 6 restriction fields to SpeciesTrait. Extractor parses allowed/forbidden origins/civics/ethics. getCompatibleTraits() now takes EmpireState. |
| 6.3 | Single Reroll Constraint | DONE | GenerationSession uses single boolean. One reroll total per generation. Frontend tooltip updated to "Reroll used". |

## Phase 7: New Empire Settings

| Task | Description | Status | Notes |
|------|-------------|--------|-------|
| 7.1 | Planet Class Model & Extractor | DONE | PlanetClass record + PlanetClassExtractor (colonizable+initial=yes). 10 habitable types extracted. Also fixed parser for `hsv { }` / `rgb { }` value-typed blocks. |
| 7.2 | Graphical Culture (Shipset) Model & Extractor | DONE | GraphicalCulture record + GraphicalCultureExtractor. Filters NPC-only (selectable={always=no}). 22 player-selectable shipsets. |
| 7.3 | Starting Leader Model & Generation | DONE | StartingRulerTrait record + StartingRulerTraitExtractor. Filters starting_ruler_trait=yes, skips tier-2. 34 traits with leader_class, forbidden_origins, allowed_ethics. |
| 7.4 | Updated DTOs, API & GeneratedEmpire | DONE | GeneratedEmpire now has 12 fields. New DTOs (PlanetClassDto, LeaderDto). 3 new RerollCategories. EmpireCard updated with Homeworld/Shipset/Leader slots. Frontend types updated. Origin-fixed planets use hardcoded map (9 origins). |

## Phase 8: Frontend Updates for New Settings

| Task | Description | Status | Notes |
|------|-------------|--------|-------|
| 8.1 | New Empire Card Slots | DONE | Completed as part of Task 7.4 — Homeworld/Shipset/Leader slots in EmpireCard, TypeScript types updated, format.ts extended. |
| 8.2 | Single Reroll UI Update | DONE | Already working from Phase 6.3 — single boolean reroll, all dice disabled after any reroll. |

## Phase 9: Bug Fixes & Data Quality (ISSUES.md)

*Species class model/extractor/filtering was completed in Phase 7+8 (SpeciesClass.java, SpeciesClassExtractor.java, allowedSpeciesClasses on SpeciesTrait, pickSpeciesClass in generator, TraitsSlot display).*

| Task | Description | Status | Notes |
|------|-------------|--------|-------|
| 9.1 | Fix country_type evaluation in EmpireState | DONE | Return Set.of("default") for COUNTRY_TYPE, hasCategory returns true. Filters ~40+ non-player civics. |
| 9.2 | Handle cross-category OR blocks in RequirementBlockParser | DONE | Added crossCategoryOrs to RequirementBlock, parseCrossCategoryOr in parser, evaluator handles OR disjunctions. |
| 9.3 | Exclude ROBOT archetype + post-validate civics | DONE | ROBOT excluded from getSelectableArchetypes. Post-validation of civics after archetype selection. Test updated. |
| 9.4 | Fix leader trait extraction — add 4 missing fields | DONE | Added allowedOrigins, allowedCivics, forbiddenCivics, forbiddenEthics to StartingRulerTrait. Extracts + filters all 6 fields. |
| 9.5 | Fix shipset extraction — filter non-shipset cultures | DONE | Excluded solarpunk_01, wilderness_01 (city-set-only). Count drops from 22 to 20. |
| 9.6 | Frontend display fixes | DONE | DLC sublabel "Requires X DLC", origin_default weight capped to 5, trait_robot_/trait_machine_ prefixes, shipset display names. |
| 9.7 | Generate core_logic.md | DONE | Created core_logic.md with pipeline description, caching, update plan. Added to .gitignore. |

## Phase 10: Polish & Issue Fixes

| Task | Description | Status | Notes |
|------|-------------|--------|-------|
| 10.1 | Localization Service Fixes | DONE | Fixed LINE_PATTERN regex (`:` without digit), added `$variable$` resolution (two-pass). 4 dedicated tests. |
| 10.2 | Homeworld Trait Constraint | DONE | Added `allowedPlanetClasses` to SpeciesTrait. Generator & reroll constrain homeworld to trait-compatible planets (e.g., Aquatic → Ocean). |
| 10.3 | Trait Points Display Fix | DONE | TraitsSlot now shows "{picks}/{max} picks · {remaining} pts remaining" instead of ambiguous "used / budget". |
| 10.4 | Multi-Species Origins | NOT STARTED | Necrophage, Syncretic Evolution, Rogue Servitor, Driven Assimilator need secondary species generation. |
| 10.5 | Error Handling & Edge Cases | NOT STARTED | Depends on 10.4 |
| 10.6 | Tauri Build & Sidecar Packaging | NOT STARTED | Depends on 10.5 |

---

## Investigation: Post-Playtest Issues (2026-02-11)

### Issue 1: No Gestalt Consciousness Empires

**Root Cause:** `CompatibilityFilterService.getRegularEthics()` (line 71) explicitly filters out gestalt with `!e.isGestalt()`. `EmpireGeneratorService.pickEthics()` only calls `getRegularEthics()` — gestalt never enters the candidate pool.

**What already exists (unreachable):**
- `getGestaltEthic()` — retrieves gestalt ethic (never called in generation)
- `getGestaltAuthorities()` — retrieves hive mind + machine intelligence (never called)
- `pickArchetype()` lines 170-177 — correctly branches for gestalt (MACHINE/ROBOT for MI, non-robotic for HM) but unreachable

**Fix (Task 6.1):** Add ~15% gestalt branch in `pickEthics()`. When gestalt is chosen:
1. Ethics = `[ethic_gestalt_consciousness]` (cost 3, fills budget alone)
2. Authority = random from `getGestaltAuthorities()` (hive mind or machine intelligence)
3. Civics = from gestalt civic pool (already handled by requirement evaluator — gestalt civics have `potential = { ethics = { value = ethic_gestalt_consciousness } }`)
4. Origin = filtered by gestalt compatibility (already works)
5. Archetype = existing logic already handles this
6. Traits = filtered by archetype (machine traits for MI, biological for HM)

### Issue 2: Missing Origin/Civic/Ethic-Specific Trait Filtering

**Root Cause:** `SpeciesTrait` model only has `allowedArchetypes`. The game uses 6 additional restriction fields documented in `000_documentation_species_traits.txt`:
- `allowed_origins` / `forbidden_origins`
- `allowed_civics` / `forbidden_civics`
- `allowed_ethics` / `forbidden_ethics`

**Current impact:** ~6 Overtuned-specific traits have `allowed_origins = { origin_overtuned }`. Currently most are filtered by `auto_mod = yes`, but `trait_spliced_adaptability` and `trait_juiced_power` (cost=1 each) may leak into non-Overtuned empires. More critically: traits that should appear FOR Overtuned empires are not being offered.

**Fix (Task 6.2):**
1. Add 6 fields to `SpeciesTrait` record
2. Update `SpeciesTraitExtractor` to parse `allowed_origins`, `forbidden_origins`, etc.
3. Change `getCompatibleTraits(String archetypeId)` → `getCompatibleTraits(String archetypeId, EmpireState state)`
4. Filter: if `allowedOrigins` is non-empty, require `state.origin()` to be in the list
5. Filter: if `forbiddenOrigins` is non-empty, require `state.origin()` to NOT be in the list
6. Same logic for civics and ethics filters

### Issue 3: Reroll Should Be Single-Use (Not Per-Category)

**Root Cause:** `GenerationSession` uses `EnumSet<RerollCategory> rerollsUsed`. Each of the 6 categories can be rerolled independently — `canReroll(cat)` checks `!rerollsUsed.contains(cat)`.

**Desired behavior:** Player gets ONE reroll total. After using it on any category, all other dice buttons become unavailable. This forces meaningful decision-making.

**Fix (Task 6.3):**
1. Replace `Set<RerollCategory> rerollsUsed` with `boolean hasRerolled`
2. `canReroll()` returns `!hasRerolled` (no category parameter needed)
3. `markRerolled()` sets `hasRerolled = true`
4. `buildRerollMap()` returns all-false after any single reroll
5. Frontend already disables all buttons when any reroll is in flight — just needs to also disable when `rerollsAvailable` is all-false (already works)

### Issue 5: Species-Specific Traits Assigned to Wrong Species (2026-02-11)

**Reported:** Lithoid species getting `trait_camouflage` (Reptilian/Aquatic/Arthropoid only) and `trait_permeable_skin` (Aquatic/Molluscoid only).

**Root Cause:** Stellaris traits have TWO-LEVEL species restrictions:
1. `allowed_archetypes = { BIOLOGICAL LITHOID }` — broad category (our code checks this ✓)
2. `species_class = { AQUATIC ART REP }` — specific species class (our code does NOT check this ✗)

Example: `trait_camouflage` has `allowed_archetypes = { BIOLOGICAL LITHOID }` AND `species_class = { AQUATIC ART REP }`. A Lithoid passes the archetype check (LITHOID is in allowed_archetypes) but should FAIL because species class LITHOID is NOT in `{ AQUATIC ART REP }`.

**Species Class → Archetype Mapping (playable):**
| Species Class | Archetype | DLC |
|---|---|---|
| HUM, MAM, REP, AVI, ART, MOL, FUN | BIOLOGICAL | Base |
| PLANT | BIOLOGICAL | Plantoids |
| NECROID | BIOLOGICAL | Necroids |
| AQUATIC | BIOLOGICAL | Aquatics |
| TOX | BIOLOGICAL | Toxoids |
| INF | BIOLOGICAL | Infernals |
| BIOGENESIS_01 | BIOLOGICAL | BioGenesis |
| MINDWARDEN | BIOLOGICAL | The Shroud |
| LITHOID | LITHOID | Lithoids |
| ROBOT | ROBOT | Base |
| MACHINE | MACHINE | Synthetic Dawn |

**Trait `species_class` Syntax:** `species_class = { CLASS_ID1 CLASS_ID2 ... }` — space-separated. Empty = unrestricted within allowed archetypes.

**Scope:** ~67 trait occurrences of `species_class` across 6 files (04_species_traits, 09_ascension_traits, 12_astral_planes_traits, 15_biogenesis_species_traits, 16_infernals_traits, 17_shroud_species_traits).

**What's Missing:**
1. `SpeciesTrait` model has NO `allowedSpeciesClasses` field
2. `SpeciesTraitExtractor` does NOT parse `species_class` from traits
3. Generator picks species ARCHETYPE but never picks a species CLASS
4. `EmpireState.speciesClass` field EXISTS but is NEVER populated during generation
5. `CompatibilityFilterService.getCompatibleTraits()` does NOT filter by species class

**Fix: Phase 9** — 3 tasks:
1. Extract species classes from game files, add `allowedSpeciesClasses` to `SpeciesTrait`, parse it
2. Pick a random species class during generation, filter traits by it
3. Update API DTOs and frontend to display species class

### Issue 4: New Empire Settings Needed

**Shipset (Graphical Culture):**
- 13 player-selectable: mammalian_01, reptilian_01, avian_01, molluscoid_01, fungoid_01, arthropoid_01, humanoid_01, plantoid_01, lithoid_01, necroid_01, aquatic_01, toxoid_01, subterranean_01
- Origin restrictions: biogenesis origins (origin_red_giant, origin_cosmic_dawn, origin_fear_of_the_dark) block biogenesis_01/02 via `graphical_culture` requirement blocks
- Random selection from compatible pool

**Homeworld Planet Type:**
- 10 standard types: pc_desert, pc_arid, pc_savannah, pc_tropical, pc_continental, pc_ocean, pc_tundra, pc_arctic, pc_alpine, pc_volcanic
- Origins that fix planet type (skip selection): Life Seeded (gaia), Void Dwellers (habitat), Post-Apocalyptic (nuked), Machine (machine world), Remnants (relic), Shattered Ring (ring segment), Ocean Paradise (ocean), Void Machines (habitat), Ocean Machines (ocean), Red Giant/Cosmic Dawn (volcanic)
- Extract from `common/planet_classes/` — `colonizable = yes` + `initial = yes`

**Starting Leader:**
- 3 classes: Official, Commander, Scientist
- Starting ruler traits in `00_starting_ruler_traits.txt` with `starting_ruler_trait = yes`
- Class-specific traits (e.g., `leader_trait_fleet_organizer` = Commander only, `leader_trait_spark_of_genius` = Scientist only)
- Multi-class traits (e.g., `trait_ruler_charismatic` = all classes)
- Some origins forbid certain starting traits (origin_legendary_leader, origin_treasure_hunters)
- Gestalt empires get `trait_ruler_feedback_loop` instead

---

## Decisions Log

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-02-10 | Project docs created | CLAUDE.md, REQUIREMENTS.md, IMPLEMENTATION_PLAN.md, PROGRESS.md |
| 2026-02-10 | Gradle over Maven | CLAUDE.md specifies Gradle 9.3+ |
| 2026-02-10 | Git initialized | Track changes per implementation plan task |
| 2026-02-10 | Jackson 3.x namespace | Spring Boot 4.0.2 uses Jackson 3 (tools.jackson.* package, not com.fasterxml.*). JsonMapper.builder().build() instead of new ObjectMapper(). |
| 2026-02-10 | Extended identifier chars | Clausewitz format uses \|, /, ' in identifiers (value:x\|y\|, trait/icon, etc.) — added to tokenizer isIdentPart. |
| 2026-02-10 | application.yml over .properties | YAML is more readable for nested config (stellaris.*, spring.web.cors.*). Switched from .properties. |
| 2026-02-10 | Lombok adoption | @Slf4j, @RequiredArgsConstructor, @Getter reduce service/controller boilerplate. Records kept for DTOs. |
| 2026-02-10 | Configurable settings plan | game-path/cache-path defaults in application.yml. Phase 10 adds settings.json + frontend settings page for user overrides. |
| 2026-02-11 | Task 3.1 pulled into Phase 2 | RequirementBlock model needed by Authority/Civic/Origin extractors, so built as prerequisite step A of Phase 2. |
| 2026-02-11 | auto_mod filter for traits | Traits with `auto_mod = yes` must be filtered even if `initial` is not explicitly `no`. |
| 2026-02-11 | Authority AI filter refined | Cannot filter on `hasCategory(COUNTRY_TYPE)` alone — auth_corporate has `country_type = { NOT = { value = primitive } }`. Must check for `Value("ai_empire")` specifically. |
| 2026-02-11 | Civic dedup in filter | CompatibilityFilterService must exclude already-selected civics from candidates — requirement blocks don't self-exclude. |
| 2026-02-11 | Gestalt empires must be supported | Playtest revealed 0% gestalt generation. Infrastructure exists (getGestaltEthic, getGestaltAuthorities, archetype branching) but pickEthics() never selects gestalt. Add ~15% branch. |
| 2026-02-11 | Origin-aware trait filtering | SpeciesTrait needs allowedOrigins/forbiddenOrigins + 4 more restriction fields per game docs. Fixes Overtuned and other origin-specific traits. |
| 2026-02-11 | Single reroll, not per-category | Player should get ONE reroll total across all categories. Forces meaningful trade-off decisions. |
| 2026-02-11 | New empire settings planned | Shipset (13 selectable), homeworld planet (10 types, some origin-fixed), starting leader class + trait. Phase 7. |
| 2026-02-11 | Parser fix: value-typed blocks | `hsv { 0.5 0.3 0.7 }` / `rgb { }` syntax was breaking the Clausewitz parser. Fixed by consuming `{ block }` after scalar value when next token is OPEN_BRACE. Planet_classes files were ALL failing before this fix. |
| 2026-02-11 | DirectoryLoader resilience | Added try-catch around individual file parsing in DirectoryLoader. Skips unparseable files with a warning instead of failing the entire directory. |
| 2026-02-11 | Species class trait filtering needed | Traits have `species_class = { }` restriction in addition to `allowed_archetypes`. ~67 trait entries have species_class restrictions. Generator must pick a species class and filter traits accordingly. Phase 9. |

## Blockers & Issues

| Issue | Status | Notes |
|-------|--------|-------|
| Windows SDK missing | CLOSED | Tauri Rust build fails — `kernel32.lib` not found. Need to install Windows 11 SDK via VS Installer |

## Session History

| # | Date | Tasks Covered | Summary                                                                                                                                                                                                                                                                       |
|---|------|---------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 | 2026-02-10 | Planning only | Created project docs: CLAUDE.md, REQUIREMENTS.md, IMPLEMENTATION_PLAN.md, PROGRESS.md. Analyzed Stellaris game files.                                                                                                                                                         |
| 2 | 2026-02-10 | 0.0–0.3, 5.2 | Scaffolding: backend verified, Tauri initialized, sidecar code + health hook written. Git repo initialized. Blocker: Windows SDK needed for Tauri build.                                                                                                                      |
| 3 | 2026-02-10 | 1.1–1.4 | Phase 1 complete: Clausewitz tokenizer, AST parser, multi-file loader, version detection & cache. 16 production files, 9 test files. Full suite passes in <5s. Pushed.                                                                                                        |
| 4 | 2026-02-10 | Arch cleanup | Converted application.properties→yml. Added Lombok (@Slf4j, @RequiredArgsConstructor, @Getter) to 5 classes. Updated CLAUDE.md, README.md, IMPLEMENTATION_PLAN.md, PROGRESS.md, LEARNING.md for consistency. Planned configurable settings for Phase 6.                       |
| 5 | 2026-02-11 | 2.1–2.5, 3.1 | Phase 2 complete. RequirementBlock model + parser (3.1 pulled forward). 6 entity models, 6 extractors, 7 test files. 25 new files, 1440 LOC. Extraction counts: 17 ethics, 7 authorities, 265 civics, 58 origins, 6 archetypes, 154 traits. All tests pass, bootRun verified. |
| 6 | 2026-02-11 | 3.2–3.3 | Phase 3 complete. EmpireState, RequirementEvaluator (pattern matching on sealed Requirement), CompatibilityFilterService. 5 new files, 25 tests. Fixed civic dedup in filter.                                                                                                 |
| 7 | 2026-02-11 | 4.1–4.3 | Phase 4 complete. EmpireGeneratorService (weighted random, ethics axis detection), RerollService (per-category reroll with locked selections), EmpireController REST API. 12 new files, 119 tests. Fixed isSameAxis bug for fanatic ethics.                                   |
| 8 | 2026-02-11 | 5.1, 5.3–5.5 | Phase 5 complete. shadcn/ui + Zustand + lucide-react. 14 new components, typed API client, Zustand store. Always-dark Stellaris theme. Empire card with ethics/authority/civics/origin/traits slots, generate + per-slot reroll buttons. 5 commits, clean build.              |
| 9 | 2026-02-11 | Investigation | Post-playtest review: 4 issues found. (1) Gestalt empires never generated. (2) Origin-specific traits not filtered. (3) Reroll should be single-use. (4) Need shipset, homeworld, leader. Planned Phase 6 (3 fix tasks), Phase 7 (4 new tasks), Phase 8 (2 frontend tasks).   |
| 10 | 2026-02-11 | 6.1–6.3 | Phase 6 complete. Gestalt generation (~20% chance), origin/civic/ethic-aware trait filtering (6 new fields on SpeciesTrait), single-reroll constraint (boolean replaces EnumSet). 3 commits, all tests pass.                                                                  |
| 11 | 2026-02-11 | 7.1–7.4, 8.1–8.2 | Phase 7+8 complete. New empire settings: homeworld (10 habitable + 9 origin-fixed), shipset (22 selectable), starting leader (3 classes + 34 traits). Fixed Clausewitz parser for `hsv { }` value-typed blocks. DirectoryLoader resilience for unparseable files. 9 new files, all 456 tests pass. |
| 12 | 2026-02-11 | Investigation | Species-class trait bug: traits with `species_class = { }` restrictions not enforced. Lithoid getting Reptilian/Aquatic-only traits. Root cause: SpeciesTrait missing allowedSpeciesClasses field, generator never picks species class. Planned Phase 9 (3 tasks). |
| 13 | 2026-02-12 | 9.1–9.7 | Phase 9 complete. 13 issues fixed: country_type eval filters ~40+ non-player civics, cross-category OR blocks for corporate civics, ROBOT archetype excluded + civic post-validation, 4 new leader trait restriction fields, shipset non-shipset filter, frontend display fixes (DLC sublabel, origin_default weight, trait prefixes, shipset names), core_logic.md. All tests pass. |
| 14 | 2026-02-16 | 10.1–10.3 | Phase 10 partial. LocalizationService: fixed regex for `:` without digit, added `$variable$` resolution (two-pass). Homeworld: added `allowedPlanetClasses` to SpeciesTrait, generator+reroll constrain homeworld by traits. TraitsSlot: now shows picks + pts remaining. 5 issues resolved, all tests pass. |
