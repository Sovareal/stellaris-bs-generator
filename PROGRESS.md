# Stellaris BS Empire Generator - Progress Tracker

> **How to use:** Update status and notes as each task is completed. This file is the
> primary context anchor across sessions — read it first to understand where we left off.

## Current Session Focus
Post-Phase 5 playtest review. Four issues identified. Planning Phase 6 (generation fixes) and Phase 7 (new empire settings).

## Last Completed Task
Phase 5 — Frontend UI. Investigation of 4 issues from playtesting complete.

## Next Up
Phase 6: Tasks 6.1–6.3 — Gestalt empires, origin-specific traits, single-reroll fix
Phase 7: Tasks 7.1–7.4 — Shipset, homeworld planet, starting leader generation
Phase 8: Tasks 8.1–8.2 — Frontend updates for new settings

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
| 6.1 | Gestalt Empire Generation | NOT STARTED | Enable gestalt consciousness ethic selection (~15% chance). Branch to hive mind / machine intelligence authorities, gestalt civics, appropriate archetypes. Infrastructure exists but is bypassed. |
| 6.2 | Origin-Aware Trait Filtering | NOT STARTED | Add allowedOrigins/forbiddenOrigins/allowedEthics/forbiddenEthics/allowedCivics/forbiddenCivics to SpeciesTrait model + extractor. Update getCompatibleTraits() to accept EmpireState. Fixes: Overtuned traits only for Overtuned origin, etc. |
| 6.3 | Single Reroll Constraint | NOT STARTED | Replace per-category reroll tracking with single-use boolean. One reroll across entire empire per generation. Update backend GenerationSession + frontend reroll map. |

## Phase 7: New Empire Settings

| Task | Description | Status | Notes |
|------|-------------|--------|-------|
| 7.1 | Planet Class Model & Extractor | NOT STARTED | Extract 10 habitable planet types (initial=yes) from common/planet_classes/. Add homeworld field to GeneratedEmpire. Origins with fixed planets (Life Seeded→gaia, Void Dwellers→habitat, etc.) skip selection. |
| 7.2 | Graphical Culture (Shipset) Model & Extractor | NOT STARTED | Extract 13 player-selectable shipsets from common/graphical_culture/. Handle origin restrictions (biogenesis origins block biogenesis_01/02). Add shipset field to GeneratedEmpire. |
| 7.3 | Starting Leader Model & Generation | NOT STARTED | Extract 3 leader classes (Official/Commander/Scientist) and starting ruler traits from common/traits/00_starting_ruler_traits.txt. Class-specific and multi-class traits. Some forbidden for specific origins. |
| 7.4 | Updated DTOs & API | NOT STARTED | Add homeworld, shipset, leader class + trait to EmpireResponse DTO. Update generate/reroll endpoints. New RerollCategories for new fields. |

## Phase 8: Frontend Updates for New Settings

| Task | Description | Status | Notes |
|------|-------------|--------|-------|
| 8.1 | New Empire Card Slots | NOT STARTED | Add Homeworld, Shipset, Leader Class + Trait slots to EmpireCard. Update TypeScript types. |
| 8.2 | Single Reroll UI Update | NOT STARTED | After any reroll, disable ALL dice buttons. Update tooltip text. |

## Phase 9: Polish & Packaging

| Task | Description | Status | Notes |
|------|-------------|--------|-------|
| 9.1 | Localization Display Names | NOT STARTED | Depends on Phase 7 |
| 9.2 | Error Handling & Edge Cases | NOT STARTED | Depends on 9.1 |
| 9.3 | Tauri Build & Sidecar Packaging | NOT STARTED | Depends on 9.2 |

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
| 2026-02-10 | Configurable settings plan | game-path/cache-path defaults in application.yml. Phase 9 adds settings.json + frontend settings page for user overrides. |
| 2026-02-11 | Task 3.1 pulled into Phase 2 | RequirementBlock model needed by Authority/Civic/Origin extractors, so built as prerequisite step A of Phase 2. |
| 2026-02-11 | auto_mod filter for traits | Traits with `auto_mod = yes` must be filtered even if `initial` is not explicitly `no`. |
| 2026-02-11 | Authority AI filter refined | Cannot filter on `hasCategory(COUNTRY_TYPE)` alone — auth_corporate has `country_type = { NOT = { value = primitive } }`. Must check for `Value("ai_empire")` specifically. |
| 2026-02-11 | Civic dedup in filter | CompatibilityFilterService must exclude already-selected civics from candidates — requirement blocks don't self-exclude. |
| 2026-02-11 | Gestalt empires must be supported | Playtest revealed 0% gestalt generation. Infrastructure exists (getGestaltEthic, getGestaltAuthorities, archetype branching) but pickEthics() never selects gestalt. Add ~15% branch. |
| 2026-02-11 | Origin-aware trait filtering | SpeciesTrait needs allowedOrigins/forbiddenOrigins + 4 more restriction fields per game docs. Fixes Overtuned and other origin-specific traits. |
| 2026-02-11 | Single reroll, not per-category | Player should get ONE reroll total across all categories. Forces meaningful trade-off decisions. |
| 2026-02-11 | New empire settings planned | Shipset (13 selectable), homeworld planet (10 types, some origin-fixed), starting leader class + trait. Phase 7. |

## Blockers & Issues

| Issue | Status | Notes |
|-------|--------|-------|
| Windows SDK missing | OPEN | Tauri Rust build fails — `kernel32.lib` not found. Need to install Windows 11 SDK via VS Installer |

## Session History

| # | Date | Tasks Covered | Summary |
|---|------|---------------|---------|
| 1 | 2026-02-10 | Planning only | Created project docs: CLAUDE.md, REQUIREMENTS.md, IMPLEMENTATION_PLAN.md, PROGRESS.md. Analyzed Stellaris game files. |
| 2 | 2026-02-10 | 0.0–0.3, 5.2 | Scaffolding: backend verified, Tauri initialized, sidecar code + health hook written. Git repo initialized. Blocker: Windows SDK needed for Tauri build. |
| 3 | 2026-02-10 | 1.1–1.4 | Phase 1 complete: Clausewitz tokenizer, AST parser, multi-file loader, version detection & cache. 16 production files, 9 test files. Full suite passes in <5s. Pushed. |
| 4 | 2026-02-10 | Arch cleanup | Converted application.properties→yml. Added Lombok (@Slf4j, @RequiredArgsConstructor, @Getter) to 5 classes. Updated CLAUDE.md, README.md, IMPLEMENTATION_PLAN.md, PROGRESS.md, LEARNING.md for consistency. Planned configurable settings for Phase 6. |
| 5 | 2026-02-11 | 2.1–2.5, 3.1 | Phase 2 complete. RequirementBlock model + parser (3.1 pulled forward). 6 entity models, 6 extractors, 7 test files. 25 new files, 1440 LOC. Extraction counts: 17 ethics, 7 authorities, 265 civics, 58 origins, 6 archetypes, 154 traits. All tests pass, bootRun verified. |
| 6 | 2026-02-11 | 3.2–3.3 | Phase 3 complete. EmpireState, RequirementEvaluator (pattern matching on sealed Requirement), CompatibilityFilterService. 5 new files, 25 tests. Fixed civic dedup in filter. |
| 7 | 2026-02-11 | 4.1–4.3 | Phase 4 complete. EmpireGeneratorService (weighted random, ethics axis detection), RerollService (per-category reroll with locked selections), EmpireController REST API. 12 new files, 119 tests. Fixed isSameAxis bug for fanatic ethics. |
| 8 | 2026-02-11 | 5.1, 5.3–5.5 | Phase 5 complete. shadcn/ui + Zustand + lucide-react. 14 new components, typed API client, Zustand store. Always-dark Stellaris theme. Empire card with ethics/authority/civics/origin/traits slots, generate + per-slot reroll buttons. 5 commits, clean build. |
| 9 | 2026-02-11 | Investigation | Post-playtest review: 4 issues found. (1) Gestalt empires never generated. (2) Origin-specific traits not filtered. (3) Reroll should be single-use. (4) Need shipset, homeworld, leader. Planned Phase 6 (3 fix tasks), Phase 7 (4 new tasks), Phase 8 (2 frontend tasks). |
