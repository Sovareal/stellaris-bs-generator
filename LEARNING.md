# Learning Log

Personal notes on tools used, lessons learned, optimization ideas, and Claude usage tips per phase.

> **Template for each phase:** Tools Used → What Went Well → Blockers → How to Optimize → Claude Cost-Effectiveness

---

## Phase 0: Project Scaffolding

### Tools Used
| Tool / Command | Purpose |
|----------------|---------|
| `gradle init` (manual build.gradle.kts) | Backend project setup — Spring Boot 4.0.2, Java 25 toolchain |
| `npm create vite` (pre-scaffolded) | Frontend React 19 + TypeScript + Tailwind CSS |
| `npx tauri init --ci` | Tauri 2.0 shell — generated src-tauri/ with Cargo.toml, lib.rs, main.rs, tauri.conf.json |
| `npm install @tauri-apps/cli @tauri-apps/api @tauri-apps/plugin-shell` | Tauri dependencies (CLI as devDep, API + shell plugin as runtime) |
| `git init` | Repository setup with .gitignore for Java/Node/Rust/IDE artifacts |
| `gradle :backend:bootJar` / `gradle :backend:bootRun` | Verify backend compiles and starts on :8080 |
| `npx tsc --noEmit` | Type-check frontend without emitting files |

### What Went Well
- Backend scaffolding was smooth — Gradle + Spring Boot 4.0 just works with Java 25.
- Tauri `--ci` flag allowed non-interactive init, which is great for scripted setup.
- Frontend type-checking caught issues early without needing a full build.

### Blocker Encountered
- **Tauri Rust build failed** — Windows SDK (`kernel32.lib`) not installed. MSVC linker was found but had no system libraries to link against.
- **Root cause:** VS 2022 Community had C++ tools but not the Windows 10/11 SDK component.
- **Fix:** Install Windows SDK via VS Installer > Individual Components > "Windows 11 SDK (10.0.x)".
- **Lesson:** Before starting a Tauri project on Windows, verify the full toolchain: `rustc`, `cargo`, MSVC linker, AND Windows SDK. The Tauri docs list prerequisites but it's easy to miss the SDK.

### How to Optimize This Phase
1. **Pre-check all prerequisites upfront** — run a single validation script that checks java, node, rustc, cargo, MSVC link.exe, and kernel32.lib before writing any code.
2. **Use a project template** — a Tauri + React + Spring Boot monorepo template would skip ~80% of Phase 0. Consider creating one after this project.
3. **Batch the Tauri init with config** — instead of init then edit tauri.conf.json/Cargo.toml separately, prepare the files upfront and write them directly.
4. **Git init first** — initialize the repo before any scaffolding so the very first commit is clean, not retroactive.
5. **Avoid background build attempts on unknown environments** — the four failed Tauri builds consumed time. A quick `link.exe /? && where kernel32.lib` check would have caught the issue in seconds.

### Claude Cost-Effectiveness
**Context used:** ~50% of 200k window for Phase 0. Expensive because of failed Tauri builds flooding context with long error output.

**What burned tokens unnecessarily:**
- Four background Tauri build attempts that all failed — each dumped ~5k tokens of linker errors into context. A 10-token prerequisite check would have prevented all of them.
- Reading back large build outputs to diagnose what was already obvious from the first failure.
- The Explore subagent was useful for initial survey but could have been replaced by 3-4 targeted Glob/Read calls for a known project structure.

**Recommendations for scaffolding phases:**
1. **Run prerequisite checks first, code second.** A single Bash call to validate all tools (`java --version && node --version && rustc --version && where link.exe && where kernel32.lib`) costs almost nothing and prevents cascading failures.
2. **Use Haiku subagents for boilerplate.** Scaffolding tasks (generate .gitignore, write config files, create directory structure) don't need Opus. Delegate to `model: "haiku"` subagents to cut cost ~20x on those subtasks.
3. **Write files directly instead of init-then-edit.** The Tauri init generated defaults that were immediately edited. Writing `tauri.conf.json`, `Cargo.toml`, `lib.rs` from scratch in one shot saves a read + edit cycle per file.
4. **Fail fast on builds.** Don't retry failed builds in background. If the first attempt fails, diagnose in the main thread before retrying — background retries burn tokens while you wait.
5. **Batch commits.** For scaffolding, prepare all files first, then commit in bulk at the end. The per-task commit granularity is better suited for logic-heavy phases (parser, rule engine) than boilerplate.

---

## Phase 1: Clausewitz File Parser

### Tools Used
| Tool / Command | Purpose |
|----------------|---------|
| `gradle :backend:test --tests "*SpecificTest"` | Incremental test execution per task |
| `gradle :backend:test` | Full suite validation after all tasks |
| `Read` with `limit: 80` | Sampled game files for format understanding |
| `Grep` | Found special characters in real game files (|, /) |

### What Went Well
- Sequential 1.1→1.2→1.3→1.4 flow was clean — each task built directly on the previous.
- Writing all files for a task at once then testing was efficient.
- Real game file integration tests caught tokenizer gaps immediately.
- Full suite stayed under 5s throughout.

### Issues Encountered
1. **Jackson 3.x namespace change** — Spring Boot 4.0.2 ships Jackson 3 (`tools.jackson.*` instead of `com.fasterxml.jackson.*`). `ObjectMapper()` constructor is deprecated; use `JsonMapper.builder().build()`. Exception handling also changed: `readValue` throws unchecked `JacksonException` not checked `IOException`.
2. **Clausewitz special characters** — Real game files use `|` (value references like `value:x|y|z|`), `/` (script paths like `trait/icon`), and `'` in identifiers. Initial tokenizer only allowed `[a-zA-Z0-9_\-\.:]`. Fixed by extending `isIdentPart`.

### How to Optimize This Phase
1. **Check real game files early.** The tokenizer worked on test snippets but broke on real files. Running the integration test with real game files after Task 1.1 (not just 1.3) would catch special chars sooner.
2. **Jackson 3 is well-documented — check before coding.** A quick web search for "Jackson 3 migration" before writing imports would have saved the compile-fix cycle.

### Claude Cost-Effectiveness
**Context used:** Moderate. 4 tasks completed in one session.

**What worked:**
- Incremental `--tests` flag kept test output small.
- `limit: 80` on game file reads avoided flooding context.
- Writing complete files with `Write` instead of incremental edits was faster.

**What could be better:**
- Could have delegated the boilerplate cache/version test files to Haiku subagents.
- The Jackson 3 compile errors took 2 fix-compile cycles — checking Spring Boot 4 docs upfront would save this.

---

## Architecture Cleanup (Between Phase 1 and 2)

### Changes Made
| Change | Before | After |
|--------|--------|-------|
| Config format | `application.properties` (flat key=value) | `application.yml` (structured YAML) |
| Logger declarations | Manual `private static final Logger log = LoggerFactory.getLogger(...)` | Lombok `@Slf4j` |
| Constructor injection | Manual constructor per service | Lombok `@RequiredArgsConstructor` |
| Getter methods | Manual `getX()` methods | Lombok `@Getter` on fields |

### Classes Modified
- `DirectoryLoader` — `@Slf4j`
- `GameFileService` — `@Slf4j` + `@RequiredArgsConstructor` + `@Getter` (removed 5 manual getters)
- `ParsedDataCache` — `@Slf4j` (constructor kept manual due to Path derivation logic)
- `GameDataManager` — `@Slf4j` + `@RequiredArgsConstructor` + `@Getter`
- `DataController` — `@RequiredArgsConstructor`

### Lessons
1. **Lombok + Records coexist well.** Records for immutable DTOs/value objects, Lombok for service/controller boilerplate. Don't force `@Data` where a record works.
2. **`@RequiredArgsConstructor` doesn't work when constructors have logic.** `ParsedDataCache` derives `cachePath` from `ParserProperties` — needs manual constructor.
3. **YAML > Properties for nested config.** `stellaris.game-path` is cleaner in YAML than flat properties, especially as config grows.

### Configurability Plan
- **Now:** Defaults in `application.yml`. Override via env vars (`STELLARIS_GAME_PATH`) or CLI args (`--stellaris.game-path=...`).
- **Phase 6:** Add `settings.json` in user data dir + frontend settings page. Auto-detect Steam paths. First-run flow if game path is invalid.

### Cost Note
This cleanup was low-cost — 5 file edits + 1 config file replacement. No subagents needed for this scope. For Phase 2, delegate model record/extractor boilerplate to Haiku subagents as planned.

---

## Phase 2: Game Data Models & Extraction

### Tools Used
| Tool / Command | Purpose |
|----------------|---------|
| `gradle :backend:test --tests "*ExtractorTest"` | Per-extractor integration tests against real game files |
| `gradle :backend:test --tests "*RequirementBlockParserTest"` | Unit tests for requirement parser |
| `gradle :backend:bootRun` | Verified extraction counts at startup |
| `Read` with `limit: 80` | Sampled game files (ethics, authorities, civics, origins, archetypes, traits) |
| `Grep` | Found patterns: `auto_mod = yes`, `is_origin = yes`, authority IDs, `random_weight` |
| Haiku subagents | Delegated 6 model record files (pure boilerplate) |

### What Went Well
- **Requirement model as prerequisite was the right call.** Pulling Task 3.1 forward into Phase 2 avoided duplication — Authority, Civic, Origin all need `RequirementBlock` fields.
- **Haiku delegation for model records worked perfectly.** 5 parallel subagents wrote all 6 model files (Ethic, Authority, Civic, Origin, SpeciesArchetype, SpeciesTrait) while I focused on extractor logic. Saved time on pure boilerplate.
- **Integration tests against real game files caught 3 bugs immediately.** Much more valuable than synthetic unit tests alone for this kind of parsing work.
- **Boot run verification was cheap and high-value.** Seeing the actual extraction counts (17/7/265/58/6/154) in the log confirmed correctness at a glance.

### Issues Encountered
1. **Authority filter too aggressive.** Initial approach filtered any authority with `country_type` in `potential`. But `auth_corporate` has `country_type = { NOT = { value = primitive } }` (just excludes primitives, not player-relevant). Only `auth_ancient_machine_intelligence` has `country_type = { value = ai_empire }`. **Fix:** Check for `Value("ai_empire")` specifically, not just presence of the category.
2. **Auto-mod traits missing `initial = no`.** `trait_auto_mod_robotic` has `auto_mod = yes` but no `initial = no` field. The filter based solely on `initial` missed it. **Fix:** Added `auto_mod = yes` as a separate exclusion filter.
3. **Origins with zero random weight.** Some origins have `random_weight = { base = 0 }` — they exist but are disabled for random selection. Test assertion `> 0` was wrong. **Fix:** Relaxed to `>= 0`.
4. **GameDataManagerTest constructor mismatch.** Adding 6 extractor dependencies to `GameDataManager` broke the existing test's manual constructor call. **Fix:** Updated test to pass all 6 extractors.

### How to Optimize This Phase
1. **Read game files before writing extractors.** I read game files first (ethics, authorities, civics, origins, archetypes, traits) and that paid off — I caught the `auto_mod` pattern, `country_type` nuance, and `cost = { base = N }` block cost format before coding.
2. **Write all extractors, then test all at once.** Rather than test-per-extractor, writing all 6 and running tests together was efficient — the compile step caught basic issues and integration tests caught logic bugs in one batch.
3. **Sealed interfaces for requirements are perfect.** Pattern matching (`r instanceof Requirement.Value v`) makes the evaluator in Phase 3 much cleaner than if we'd used a class hierarchy.

### Claude Cost-Effectiveness
**Context used:** Moderate. Full phase done in one session.

**What worked well:**
- Haiku subagents for model records saved ~5x on boilerplate generation.
- `limit: 80` on game file reads — enough to see patterns without flooding context.
- Compile check (`compileJava`) before running tests caught basic issues cheaply.
- Batch test runs (`--tests "*ExtractorTest"`) instead of one-at-a-time.

**What could be better:**
- Could have predicted the `GameDataManagerTest` constructor break before running full suite — any time you add dependencies to a `@RequiredArgsConstructor` class, its test will break.
- The three test failures required a second compile+test cycle. Reading game files more carefully for edge cases (zero weights, auto_mod without initial=no) upfront would have avoided this.

---

## Phase 3: Rule / Constraint Engine

### Tools Used
| Tool / Command | Purpose |
|----------------|---------|
| `gradle :backend:test --tests "*RequirementEvaluatorTest"` | Unit tests for requirement evaluation |
| `gradle :backend:test --tests "*CompatibilityFilterServiceTest"` | Integration tests with full extraction chain |
| `gradle :backend:compileJava` | Quick compile checks before running tests |

### What Went Well
- **Sealed interface pattern matching paid off.** The `Requirement` sealed interface (Value, Not, Nor, Or) from Phase 2 made the evaluator clean — each case is a one-liner with `instanceof` pattern matching.
- **Deferred validation for unselected categories was the right approach.** Instead of failing when a category hasn't been selected yet, the evaluator skips it. This allows progressive narrowing: pick ethics → filter authorities → pick authority → filter civics, etc.
- **EmpireState as immutable record with `with*()` builders** kept the state management clean — no accidental mutations during filtering.

### Issues Encountered
1. **Civic deduplication missing.** `CompatibilityFilterService.getCompatibleCivics()` returned all civics matching requirements, but didn't exclude civics already selected. When picking a second civic, the first one would sometimes reappear. **Fix:** Added `.filter(c -> !state.civics().contains(c.id()))`.

### How to Optimize This Phase
1. **Phase 3 was small and focused.** 5 files, 25 tests. No real waste.
2. **The civic dedup bug could have been predicted** — any time you filter a list for a "pick next" scenario, you need to exclude already-picked items.

### Claude Cost-Effectiveness
**Context used:** Low. Phase completed quickly.
**What worked:** Writing evaluator + filter service together, testing in batch. No subagents needed — logic-heavy code that benefits from context continuity.

---

## Phase 4: Empire Generation Engine

### Tools Used
| Tool / Command | Purpose |
|----------------|---------|
| `gradle :backend:test --tests "*EmpireGeneratorServiceTest"` | Generator tests including 100-rep stress test |
| `gradle :backend:test --tests "*RerollServiceTest"` | Reroll integration tests |
| `gradle :backend:test` | Full suite validation |

### What Went Well
- **WeightedRandom utility was worth extracting.** Used by both the generator and reroll service. Simple cumulative-weight algorithm with uniform fallback when all weights are 0.
- **100-repetition stress test was high-value.** The `@RepeatedTest(100)` caught the `isSameAxis` bug that occurred in ~5% of generations. A single-run test would likely have missed it.
- **Reroll service's "locked selection" pattern worked cleanly.** Each reroll method receives the full empire, rebuilds the constraint state from locked selections, filters compatible replacements, and returns a new empire with only the target category changed.
- **DTO layer with `from()` static factory methods** keeps conversion logic close to the DTO definitions, not scattered across the controller.

### Issues Encountered
1. **isSameAxis bug for fanatic ethics.** Fanatic ethics have a `regularVariant` link but null `fanaticVariant`. The guard `a.fanaticVariant() != null && b.fanaticVariant() != null` prevented the `category` comparison from ever running for fanatic ethics. This meant `ethic_fanatic_authoritarian` + `ethic_egalitarian` could coexist. **Fix:** Removed the guard, always compare categories (`a.category().equals(b.category())`). Categories are always set for non-gestalt ethics.
2. **Ethics reroll uses retry loop.** Unlike authority/civic/origin where we can directly filter compatible options, ethics reroll generates full empires and checks if the candidate ethics are compatible with all locked selections. This is because the ethics affect everything downstream. Up to 50 attempts before throwing.

### Architecture Notes
- **Session is in-memory (single user desktop app).** No database, no concurrent sessions. `EmpireController` holds one `GenerationSession` instance.
- **Reroll categories are fine-grained:** CIVIC1 and CIVIC2 are separate categories, so you can reroll each civic independently while preserving the other.
- **Error responses use `@ControllerAdvice`:** GenerationException → 422, IllegalStateException → 409, IllegalArgumentException → 400. Structured JSON: `{"error": "...", "message": "..."}`.

### How to Optimize This Phase
1. **Write the generator and reroll together.** They share logic (filtering, weighted selection, trait picking). Writing them back-to-back in one session maintained context continuity.
2. **The ethics reroll retry loop is inelegant** but pragmatic. A more principled approach would directly enumerate compatible ethics sets, but that's complex and the retry works well in practice (games have many ethics).

### Claude Cost-Effectiveness
**Context used:** Moderate. Three tasks (4.1–4.3) completed in one session.
**What worked:**
- No subagents for Phase 4 — all logic-heavy code that benefits from seeing the full picture.
- Incremental `--tests` runs kept feedback loops tight.
- Writing complete files rather than incremental edits reduced round-trips.

---

## Phase 14+15: Display & Icons + Generation Order & Compatibility

### Tools Used
Edit, Read, Bash (gradle test, tsc --noEmit), Haiku subagents for icon pipeline

### What Went Well
1. **Real trait data for enforced traits** — switching from stub SpeciesTraits to `findTraitById()` lookups fixed both test failures and Aquatic→Ocean homeworld constraint in one change.
2. **Generation order restructure** (species archetype before origin/civics) elegantly solved multiple compatibility issues at once.
3. **Civic-enforced traits** (e.g., Anglers→Aquatic) unified with origin-enforced trait system — same code path handles both.

### Blockers
- Reroll test failures after restructure — required adding speciesArchetype/speciesClass to EmpireState in all reroll methods.
- File read requirement for Edit tool — attempted parallel edits without reading first.

### How to Optimize
- When restructuring generation order, update ALL reroll methods that build EmpireState simultaneously.
- Enforced trait stub objects are fragile — always use real game data when available.

---

## Phase 16+17: Code Quality Refactor + Habitability Preference

### Tools Used
Haiku subagent for DTO file creation (12 files), Edit, Read, Bash (gradle test, tsc)

### What Went Well
1. **Haiku subagent for DTO extraction** — 12 boilerplate files created efficiently without consuming main context.
2. **Habitability preference** three-tier logic was clean: origin explicit → fixed homeworld random → default homeworld.
3. **Plan mode** helped organize two phases into a clear dependency graph (16.1, 16.2, 17.1 parallel → 17.2 → 17.3 → 17.4).

### Blockers
None — clean execution.

### Claude Cost-Effectiveness
**Context used:** Moderate. Phase 16 was mostly mechanical (DTO extraction). Phase 17 required understanding origin/homeworld semantics.
**What worked:**
- Haiku subagent saved significant context on boilerplate DTO files.
- Combining two small phases (16+17) into one plan reduced overhead.
- All 667 tests passed on first run after implementation — good test coverage from earlier phases pays off.

## Phase 70: Dynamic Port + IPC Crash Detection

### Tools Used
Read, Write, Edit, Bash (cargo check, tsc, knip)

### What Went Well
1. **`Emitter` trait import** -- Rust compiler error was precise: "trait `Emitter` which provides `emit` is implemented but not in scope" with the exact fix `use tauri::Emitter`. Fast diagnosis.
2. **`Mutex<u16>` for port state** -- Clean solution for the constraint that `manage()` runs before `setup()`: register placeholder, overwrite in setup.
3. **Module-level promise in `api.ts`** -- `const backendPortPromise` initialized once at import time, shared by both `request()` and `useBackendReady.ts` via named export. No lifecycle complexity.
4. **knip caught stale `ignoreDependencies`** -- `@tauri-apps/api` was in the ignore list because it was unused; now imported directly, knip flagged it immediately.

### Blockers
None -- single compile error (missing `Emitter` import) caught by `cargo check`.

### Claude Cost-Effectiveness
**Context used:** Light. Files were small and well-scoped; one round of `cargo check` caught the only issue.
**What worked:**
- Reading PROGRESS.md + code review summary gave exact scope without needing to read phase files.
- Running `cargo check` + `tsc --noEmit` + `knip` in one pass validated all three layers cleanly.
