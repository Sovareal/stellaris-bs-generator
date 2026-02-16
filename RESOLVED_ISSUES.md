# Resolved Issues

## Phase 9 — Bug Fixes & Data Quality (2026-02-12)

### Issues 1, 3: Non-player civics leaking into pool (pre-FTL, fallen empire, enclave, marauder)
**Fix:** `EmpireState.valuesForCategory(COUNTRY_TYPE)` now returns `Set.of("default")` and `hasCategory(COUNTRY_TYPE)` returns `true`. Civics with `country_type = { value = primitive }` etc. now correctly fail evaluation. ~40+ invalid civics filtered.

### Issue 2, 9: Invalid starting leader traits (Legendary Foe Hammer, Treasure Hoarder)
**Fix:** Added 4 missing restriction fields to `StartingRulerTrait`: `allowedOrigins`, `allowedCivics`, `forbiddenCivics`, `forbiddenEthics`. Extractor parses all 6 fields. `getCompatibleRulerTraits()` applies all 6 filters. Legendary Leader traits now require `origin_legendary_leader`, Treasure Hunter traits require `origin_treasure_hunters`.

### Issue 4: Wrong species class prefix on traits (e.g., "Humanoid" on Lithoid)
**Fix:** Resolved by Task 9.3 — ROBOT archetype excluded from selectable archetypes. The 0/0 trait point edge case that caused fallback to empty species class is eliminated.

### Issues 5, 6: DLC/origin sublabel showing raw values ("Federations", "Apocalypse")
**Fix:** DLC sublabel now shows `"Requires X DLC"` format instead of raw `dlcRequirement` string.

### Issue 7: origin_default appearing too frequently
**Fix:** `pickOrigin()` caps `origin_default` weight to 5 (game file has 100, which dominated all other origins at ~5).

### Issues 8, 11: Robot/Machine trait prefixes, 0/0 trait points
**Fix:** (a) ROBOT archetype excluded from `getSelectableArchetypes()`. (b) Post-validation of civics after archetype selection catches `species_archetype` requirements. (c) Frontend `format.ts` now strips `trait_robot_` and `trait_machine_` prefixes.

### Issue 10: Invalid shipsets (Solarpunk, Wilderness)
**Fix:** `GraphicalCultureExtractor` excludes `solarpunk_01` and `wilderness_01` (city-set-only cultures with `generate_shipset = no`). Also added display name map in frontend for all shipset IDs.

### Issue 12: Corporate civics rolling for non-corporate empires
**Fix:** `RequirementBlockParser` now handles top-level `OR` blocks that span multiple categories. `RequirementBlock` has new `crossCategoryOrs` field. `RequirementEvaluator` checks that at least one branch of each cross-category OR is satisfied.

### Issue 13: Missing core_logic.md documentation
**Fix:** Created `core_logic.md` documenting the empire generation pipeline, data extraction flow, caching strategy, and DLC/patch update plan. Added to `.gitignore`.

## Phase 10 — Localization & Display Fixes (2026-02-16)

### Issue 1a: "Fleeting Lithoid" trait name shows species prefix
**Fix:** LocalizationService now resolves `$variable$` references instead of stripping them. `trait_fleeting_lithoid:0 "$trait_fleeting$"` resolves to "Fleeting" via two-pass loading: first pass stores raw values, second pass replaces `$key$` with resolved values.

### Issue 1b: Anglers + Aquatic + Desert homeworld
**Fix:** Added `allowedPlanetClasses` field to `SpeciesTrait` model. `SpeciesTraitExtractor` parses `allowed_planet_classes` from game files. `pickHomeworld()` now intersects habitable planets with trait constraints (e.g., Aquatic → pc_ocean only). RerollService also respects trait planet constraints.

### Issue 1c: "0 / 2 trait points" display is ambiguous
**Fix:** TraitsSlot now shows `"{picks}/{maxPicks} picks · {remaining} pts remaining"` format (e.g., "5/5 picks · 2 pts remaining").

### Issue 2a: "Void Machines" origin name not localized
**Fix:** LocalizationService LINE_PATTERN regex changed `(?:\\d+)?` to `(?:\\d*)?` to handle entries without version digit after colon (e.g., `origin_void_machines: "Voidforged"`). Key stored correctly as `origin_void_machines` instead of `origin_void_machines:`.

### Issue 2b: "Humanoid Jinxed" trait name shows species prefix
**Fix:** Same regex fix as Issue 2a. `trait_humanoid_jinxed: "Jinxed"` now stored with correct key and resolves to "Jinxed".
