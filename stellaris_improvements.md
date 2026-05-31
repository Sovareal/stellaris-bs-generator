# Stellaris BS Empire Generator - Codebase Improvements

This document outlines the architectural, performance, and code quality improvements identified in the codebase review, excluding the intentional generation logic design choices (Origin/Ethics weighting).

## 1. Concurrency Bottleneck in Generator
**Core of the issue:** `EmpireGeneratorService.java` utilizes a single `private final Random random = new Random();` instance.
**Clarification:** Spring `@Service` classes are singletons. Using `java.util.Random` across multiple threads concurrently can cause thread contention and lock overhead. While not critical for a single-user desktop app, it's an anti-pattern.
**Proposition for resolution:** Replace the shared `Random` instance with `ThreadLocalRandom.current()`. This provides thread-safe, non-blocking random number generation with zero performance overhead.

## 2. Tokenizer Memory Footprint
**Core of the issue:** `Tokenizer.tokenize()` in the custom Clausewitz AST parser loads the entire file's tokens into a `List<Token>`.
**Clarification:** For extremely large Stellaris save files or massive mod data files, instantiating millions of `Token` objects and holding them simultaneously in a list creates significant Garbage Collection (GC) pressure and memory spikes.
**Proposition for resolution:** Refactor the `Tokenizer` to implement `Iterator<Token>`. This allows the `ClausewitzParser` to stream tokens on-demand, reducing the memory footprint to a single token at any given time.

## 3. Implicit Truncation of Numbers in AST
**Core of the issue:** `ClausewitzNode.childInt()` silently casts doubles to integers: `(int) Double.parseDouble(v)`.
**Clarification:** If a game update or a mod introduces fractional costs (e.g., `cost = 1.5`), the parser will silently truncate it to `1` without any warning. This leads to inaccurate budget calculations that are difficult to debug.
**Proposition for resolution:** Switch internal mathematical evaluations to `double` or `BigDecimal` where appropriate, or at minimum, add a logging warning when `Math.abs(val - Math.round(val)) > 0` before truncating.

## 4. Inconsistent CSS / Styling Strategies
**Core of the issue:** The React frontend mixes Tailwind CSS utility classes with heavy inline style objects.
**Clarification:** Components like `App.tsx` use clean Tailwind (`className="min-h-screen bg-background flex flex-col"`), but `ConsoleButton.tsx` and `EmpireView.tsx` use large inline `style={{ ... }}` objects. This fragments the styling paradigm, making global theming and maintenance difficult.
**Proposition for resolution:** Migrate all inline styles to Tailwind CSS. Custom telemetry animations or specific gradients can be added to `tailwind.config` or as custom CSS variables in `index.css`.

## 5. Synchronous Image Conversion
**Core of the issue:** `IconService.java` converts DDS images to PNG synchronously on the main HTTP request thread using `ImageIO.read()`.
**Clarification:** When a new empire is generated and the UI requests icons that aren't cached yet, the synchronous disk I/O and image conversion cause a "waterfall" of slow image loads, making the UI feel sluggish on the first run.
**Proposition for resolution:** Move the DDS-to-PNG conversion to a background worker queue during the `GameDataManager` startup sequence (pre-warming the cache), or handle it asynchronously.

## 6. Duplicated Planet Constraint Logic
**Core of the issue:** Re-calculation of homeworld and habitability constraints is copy-pasted across `RerollService.java`.
**Clarification:** The exact same logic block for `newPlanetConstraint`, `oldPlanetConstraint`, `pickHomeworld`, and `pickHabitabilityPreference` is repeated in `rerollAuthority`, `rerollCivic`, `rerollOrigin`, `rerollSingleTrait`, `addOneTrait`, and `removeRandomTrait`.
**Proposition for resolution:** Extract this logic into a private helper method (e.g., `recalculateHomeworldIfNeeded(empire, newTraits)`) to dry up the code and prevent bugs if the planet derivation logic changes later.

## 7. API Wrapper and UI State Coupling
**Core of the issue:** `useEmpireStore.ts` mixes pure UI state with API fetching logic.
**Clarification:** State flags like `isAddingTrait` or `isRerollingTrait` are tightly bound to the `withApi` wrapper. This makes the Zustand store bloated and harder to isolate or test.
**Proposition for resolution:** Isolate server state and caching by using a library like TanStack React Query, leaving Zustand strictly for client-side UI states (like modal visibility or local session IDs).

## 8. Hardcoded File Paths in Tooling
**Core of the issue:** `NamePoolExtractorRunner.java` hardcodes a specific local drive path (`F:/Games/SteamLibrary/steamapps/common/Stellaris`) as the default.
**Clarification:** Any other developer cloning the repository will experience immediate failure when running the extractor script unless they manually edit the source code.
**Proposition for resolution:** Utilize the same auto-detection logic present in `SettingsService.detectDefaultGamePath()` to find the Stellaris directory dynamically across Windows, macOS, and Linux.

## 9. Lack of Cache on Expensive Filters
**Core of the issue:** `CompatibilityFilterService.java` performs heavy stream filtering for every generation step.
**Clarification:** Methods like `getCompatibleTraits` run complex intersections against allowed/forbidden origins, civics, and ethics. Doing this repeatedly per generation cycle is inefficient.
**Proposition for resolution:** Pre-compute and index these relationships in memory during the `GameDataManager` startup, or utilize Spring's `@Cacheable` annotations for deterministic lookup methods.

## 10. Ungraceful Backend Shutdown
**Core of the issue:** The Tauri shell forcefully terminates the Spring Boot process via `child.kill()` when the app closes.
**Clarification:** Sending a SIGKILL prevents Spring Boot from running `@PreDestroy` hooks, closing file streams properly, or flushing logs.
**Proposition for resolution:** Expose a graceful shutdown endpoint (like Spring Boot Actuator's `/actuator/shutdown`), and have Tauri send a POST request to it. Only fallback to `child.kill()` if the process doesn't terminate within a few seconds.

## 11. Export Modal Focus Trapping
**Core of the issue:** `ExportModal.tsx` lacks focus trapping.
**Clarification:** A user navigating with the keyboard (Tab key) can tab out of the open modal and interact with background elements on the main page, leading to unintended UI states.
**Proposition for resolution:** Wrap the modal contents in a focus trap utilizing Radix UI's `Dialog` component (since Radix is already in the `package.json` dependencies) to ensure accessibility compliance.
