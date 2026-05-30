# Phase Registry

Quick lookup index. Find the phase here, then read the file for full detail.
All files are in `docs/phases/`. Status: all phases DONE unless noted.

| Phase | Name | Key Decisions / Topics |
|-------|------|------------------------|
| 00 | Project Scaffolding | Gradle multi-project setup, gitignore, initial git history |
| 01 | Clausewitz File Parser | Token-based lexer, extended ident chars (`\|` `/` `'`), @variable substitution, AST recursive descent, multi-file loader, SHA-256 cache fingerprint |
| 02 | Game Data Models & Extraction | 6 entity models (Ethic, Authority, Civic, Origin, SpeciesArchetype, SpeciesTrait), RequirementBlock pulled forward from Phase 3, extraction counts |
| 03 | Rule / Constraint Engine | Sealed Requirement hierarchy, RequirementEvaluator pattern matching, CompatibilityFilterService, civic dedup in filter |
| 04 | Empire Generation Engine | EmpireGeneratorService, WeightedRandom, ethics axis detection, isSameAxis fanatic fix, RerollService per-category, EmpireController REST API |
| 05 | Frontend UI | shadcn/ui, Zustand store, empire card with slots, generate + per-slot reroll buttons, Stellaris dark theme |
| 06 | Generation Fixes | Gestalt ~20% branch added, 6 new fields on SpeciesTrait (allowed/forbidden origins/civics/ethics), single reroll boolean replaces EnumSet |
| 07 | New Empire Settings | Homeworld (10 habitable + origin-fixed), shipset (22 selectable), leader (3 classes + traits), **hsv{} value-typed block parser fix**, DirectoryLoader resilience |
| 08 | Frontend for New Settings | Homeworld, shipset, leader slots in UI |
| 09 | Species Class Trait Filtering | country_type filter removes ~40 non-player civics, corporate OR blocks, ROBOT archetype excluded, 4 leader trait restriction fields, shipset non-shipset filter, 13 fixes total |
| 10 | Polish & Packaging | LocalizationService `$variable$` two-pass resolution, `allowedPlanetClasses` on SpeciesTrait, multi-species origins (SecondarySpeciesConfig), error handling (DataStatus enum), SettingsPage, Tauri NSIS packaging (bundle-jre, jlink) |
| 11 | Display Names & Homeworld | Shipset display names (Spinovore/Shellcraft), nemesis_01 excluded, volcanic homeworld restricted to INF species, uniform origin weights |
| 12 | Origin Enforced Traits | enforcedTraitIds on Origin, free prepended traits, lock badge; Luminary leader multi-trait with budget=1 picker |
| 13 | Game Entity Icons | TwelveMonkeys DDS-to-PNG, two-tier cache (memory+disk), IconController, LeaderTraitGfxParser, EntityIcon component |
| 14 | Display & Icon Fixes | "Fanatic" prefix removed, enforced traits excluded from pick count, trait iconPath, "?" placeholder, icon sizes 24-36px |
| 15 | Generation Order Fix | **Archetype/species picked before origin/civics** (restructured order), civic-enforced traits (civic_anglers->trait_aquatic), aquatic->ocean homeworld constraint |
| 16 | Code Quality Refactor | 12 DTOs extracted to dto package, MAX_REROLL_ATTEMPTS constant |
| 17 | Habitability Preference | Three-tier logic: origin explicit -> fixed-homeworld random -> default homeworld, hab pref parsed from origins, reroll re-derives hab pref |
| 18 | Origin Extractor Fix | Filters `potential.always=no` origins (Daemonic Incursion) and `random_weight.base=0` origins (legendary_leader_death variants) |
| 19 | Reroll Origin Regen | rerollOrigin() fully regenerates: species traits from new origin, leader traits, uniform random selection |
| 20 | Icon Fixes | allTraitIconPaths map covers initial=no traits (void dweller, clone soldier, cyber commandos, unplugged) |
| 21 | Secondary Species Quality | generateSecondarySpecies uses findTraitById for real opposites/iconPath |
| 22 | Enforced Trait Cost Fix | Enforced traits show real cost (+2 for aquatic); budget calc filters enforced IDs from traitPointsUsed |
| 23 | Under One Rule Edge Cases | UOR + GC: NOR block parsing verified correct; @RepeatedTest(200) regression added |
| 24 | Homeworld on Origin Reroll | rerollOrigin() regenerates homeworld + habitability preference after origin change |
| 25 | Homeworld Constraint | rerollTraits() re-derives homeworld when planet constraints change (Aquatic + Tundra/Arctic bug) |
| 26 | GC/Gestalt Reroll | performRegimeChange() fallback in rerollEthics() for GC empires; performGestaltSwitch() in rerollAuthority() |
| 27 | UI Polish | billy.png missing icon placeholder; ruler trait badges get green background |
| 28 | Origin Distribution Diagnostic | @Disabled originDistributionReport() generates 1000 empires and logs frequency table |
| 29 | Origin Rebalancing | GESTALT_CHANCE 0.20->0.30, originRarityWeight() 2-6x boosts restricted origins, FUN/PLANT 3x class weight |
| 30 | Cross-Platform Build | bundle-jre.sh (Linux/macOS), before-build.js, tauri.conf.json targets="all", SettingsService.detectDefaultGamePath() OS-aware |
| 31 | App Branding | App icon from app_icon.jpg, favicon.png, HTML title, package.json rename, header logo |
| 32 | Origin Distribution Fix | **Positive-only restriction detection** (NOT blocks were inflating pool weights), tier 3 GESTALT_EXCLUSIVE_ORIGINS, ORIGIN_WEIGHT_OVERRIDES map for 4 stubborn origins |
| 33 | Corporate Dominion Fix | CivicExtractor filters `playable = { NOT = { host_has_dlc = "X" } }` -- civic_corporate_dominion removed |
| 34 | Per-Trait Reroll | TRAIT_SINGLE reroll category, rerollSingleTrait() budget-aware with opposites, per-trait dice buttons in UI |
| 35 | Save to Game | EmpireExporterService Clausewitz output, UserEmpireFileService (Documents/Paradox Interactive/Stellaris/), ExportModal, SaveToGameButton, StatusToast |
| 36 | Export + Budget Fixes | Enforced traits count against pick limit AND budget; export leader trait lines; computeGovernment() ethics-aware |
| 38 | Issue Fixes | StatusToast auto-dismiss on next click; enforced trait opposites seeded into excludedByOpposites in pickTraits() |
| 39 | Trait-by-Trait Rolling | Initial generation starts enforced-only; addOneTrait() one at a time; Done Rolling gates export; traitsFinalized state |
| 40 | Reroll Token Enforcement | rerollSingleTrait() consumes global reroll token; **3-case budget filter** in addOneTrait() (last pick non-negative / in-debt only negatives / otherwise any) |
| 41 | Trait Preservation | fromFreeEnforced() cost=0; preserveRandomTraits() through origin/civic rerolls; removeRandomTrait() + button; canFinalize gates picksRemaining>=0 |
| 42 | Budget Calculation Fix | fromFreeEnforced reverted to t.cost(); generationId only increments on generate/reroll (no blink); green/red color coding |
| 43 | Secondary Species Trait Rolling | Secondary species uses same trait-by-trait rolling mechanics |
| 44 | Settings Page Close Button | Close button added to settings modal |
| 45 | Shipset Enforcement + Export | Shipset constraint enforced on reroll; empirePrefix/empirySuffix export fields; leader trait badge styling |
| 46 | Animations + Civic Effects | CSS animations, civic visual effects |
| 47 | Auto-generated Empire Name | Empire name generated from name pool on generate; displayed in StatusBar |
| 48 | UI Redesign Tokens | CSS keyframes (telemetryPulse, telemetryAttn), font imports, formatSeed/formatCost/traitCostColor helpers |
| 49 | UI Redesign Primitives | Panel, MonoRow, Tag, HBar (20-seg budget meter), InlineReroll, RowReroll, ConsoleButton |
| 50 | IDEOLOGY Column | EthicsPanel, AuthorityPanel, CivicsPanel, OriginPanel |
| 51 | GENOME Column | SpeciesPanel (ports all TraitsSlot logic), SecondarySpeciesPanel |
| 52 | LOGISTICS Column | HomeworldPanel, ShipsetPanel, LeaderPanel |
| 53 | Layout Assembly | StatusBar, DesignationBanner, EmpireConsole 3-col grid, EmpireView rewrite, App.tsx gameVersion prop |
| 54 | Redesign Cleanup | Deleted legacy components: EmpireCard, EmpireSlot, EthicsSlot, TraitsSlot, SecondarySpeciesSlot, GenerateButton, SaveToGameButton, RerollButton |
| 55 | Redesign Polish | Edge cases and polish for 3-col redesign |
| 56 | Post-Redesign UX | SEED removed, HABITAT removed, generate button empty state, leader badge traits, font scale-up, settings cancel button |
| 57 | Icon + Banner Fixes | Planet icon category fix, icon sizes 20->28px, DESIGNATION->EMPIRE banner label |
| 58 | Secondary Species Display | CLASS row removed, primary ARCHETYPE row removed, origin icon 28->36px, other icons 28->32px |
| 59 | Over-Budget Trait State | Partial fix for over-budget display state |
| 60 | Dead Code Removal | preserveRandomTraits pick-count trim, dead code removal |
| 61 | Name Pool Feature | NamePoolExtractor from game files, NameGeneratorService, export integration (empirePrefix/Suffix from pool) |
| 62 | Name Pool Storage Refactor | Moved name lists from EmpireResponse to backend-owned JSON; lazy loading |
| 63 | Toast + Dead Code | Toast visibility fix, dead code removal |
| 64 | Species Class Distribution | **WeightedRandom.select() across archetypes weighted by class weights** -- fixes LITHOID ~3.8%, unweighted BIOLOGICAL classes ~3.8% each |
| 65 | Fanatic Ethics Frequency | Replaced nextBoolean() with `nextDouble() < FANATIC_ETHICS_CHANCE` (0.65) -- increases fanatic empire rate from ~4.7% to ~9.3% |
| 66 | Fanatic Pacifist Weight | FANATIC_ETHICS_WEIGHT_OVERRIDES map -- ethic_fanatic_pacifist boosted from game weight 33 to 150 |
| 67 | Security Hardening | CORS restricted to localhost:5173 + tauri://localhost; path traversal validation in IconController |
| 68 | Error Handling | SettingsCorruptedException surfaced to UI with reset flow; ApiError captures error code; file export lock retry |
| 69 | Code Quality | Zustand withApi wrapper eliminates store boilerplate; @Cacheable replaces DCL in CompatibilityFilterService |
| 70 | Dynamic Port + IPC Crash | TcpListener::bind(0) allocates free port; --server.port=PORT to Java; get_backend_port command; try_wait monitor thread emits backend-crashed; backendPortPromise in api.ts; listen in useBackendReady |
