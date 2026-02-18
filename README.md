# Stellaris BS Empire Generator

A desktop application that generates random, rule-valid Stellaris empires by parsing the game's own data files.

![App Icon](app_icon.jpg)

## How It Works

The app reads your local Stellaris installation files (Clausewitz `.txt` format) and extracts all ethics, authorities, civics, origins, species archetypes, traits, homeworlds, shipsets, and starting leader traits — along with their full compatibility rules. It then generates randomized empires that respect every constraint the game enforces.

Each generated empire includes:
- **Ethics** (cost-balanced to 3 points; ~30% chance of Gestalt Consciousness)
- **Authority** (compatible with the chosen ethics)
- **2 Civics** (respecting mutual exclusions and prerequisites)
- **Origin** (valid for the selected government; rare/restricted origins boosted)
- **Species** — archetype, class, and traits (within point budget, no conflicting opposites)
- **Homeworld** planet type (constrained by traits like Aquatic, origin-fixed for relevant origins)
- **Habitability Preference** (derived from origin or homeworld)
- **Shipset** (graphical culture)
- **Starting Leader** with class and traits
- **Secondary Species** (for multi-species origins/civics like Necrophage, Syncretic Evolution)

You get one reroll per generated empire. Rerolling any category uses it up.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 25 (Amazon Corretto), Spring Boot 4.0, Gradle, Lombok |
| Frontend | React 19, TypeScript, Tailwind CSS, shadcn/ui, Zustand |
| Desktop Shell | Tauri 2.0 (Rust) |

## Platform Support

The app builds and runs on **Windows**, **Linux**, and **macOS** — matching the platforms Stellaris officially supports.

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Java (JDK) | 25 | Needed for building. End-user installs bundle a minimal JRE — no system Java required. |
| Node.js | 22+ | |
| Rust + Cargo | stable | Via [rustup](https://rustup.rs) |
| Stellaris | any recent | Must be installed locally |

> **Windows only:** Visual Studio Build Tools or the MSVC toolchain (for Tauri's Rust compilation). Install via [Visual Studio Installer](https://visualstudio.microsoft.com/downloads/) — select "Desktop development with C++".

## Building & Running

### Development

```bash
# --- Backend ---

# Windows
gradlew.bat :backend:bootRun     # Run dev server on :8080
gradlew.bat :backend:test        # Run tests

# Linux / macOS
./gradlew :backend:bootRun
./gradlew :backend:test

# --- Frontend ---
cd frontend
npm install                      # First time only
npm run dev                      # UI only (Vite, :5173)
npm run tauri dev                # Full app (Tauri + auto-started backend sidecar)
```

In `tauri dev` mode the backend is started automatically from the pre-built JAR at
`backend/build/libs/backend-0.1.0.jar`. If it doesn't exist yet, build it first:

```bash
# Windows
gradlew.bat :backend:bootJar

# Linux / macOS
./gradlew :backend:bootJar
```

### Production Build

The build pipeline is fully automated via a cross-platform Node.js script:

```bash
cd frontend
npm run tauri build
```

This runs the following steps automatically:

1. **`gradlew :backend:bootJar`** — compiles the Spring Boot backend into a fat JAR
2. **Copy** `backend/build/libs/backend-0.1.0.jar` → `frontend/src-tauri/backend.jar`
3. **`scripts/bundle-jre.bat`** (Windows) or **`scripts/bundle-jre.sh`** (Linux/macOS) — uses `jlink` to produce a minimal JRE (~50 MB) containing only the modules Spring Boot needs
4. **`npm run build`** — compiles the React + TypeScript frontend via Vite
5. **Tauri** — compiles the Rust shell in release mode and packages everything into a platform installer

#### Output locations

| Platform | Format | Path |
|----------|--------|------|
| Windows | NSIS installer (`.exe`) | `frontend/src-tauri/target/release/bundle/nsis/` |
| Linux | AppImage + `.deb` | `frontend/src-tauri/target/release/bundle/appimage/` and `deb/` |
| macOS | `.dmg` + `.app` | `frontend/src-tauri/target/release/bundle/dmg/` and `macos/` |

### First Run

The installer bundles a minimal JRE — **no system Java required** on the end user's machine. On launch, the Tauri shell starts the Spring Boot backend using the bundled JRE and opens the UI in a native window.

The app auto-detects your Stellaris installation from common Steam paths:

| OS | Paths checked |
|----|--------------|
| Windows | `C:\Program Files (x86)\Steam\steamapps\common\Stellaris`<br>`C:\`, `D:\`, `E:\`, `F:\SteamLibrary\steamapps\common\Stellaris` |
| macOS | `~/Library/Application Support/Steam/steamapps/common/Stellaris` |
| Linux | `~/.steam/steam/steamapps/common/Stellaris`<br>`~/.local/share/Steam/steamapps/common/Stellaris`<br>`~/snap/steam/common/.steam/steam/steamapps/common/Stellaris` |

If the path is not found automatically, the app prompts you to set it via the Settings page.

## Project Structure

```
Stellaris BS Generator/
├── backend/                    # Spring Boot 4.0 app (port 8080)
│   └── src/main/java/com/stellaris/bsgenerator/
│       ├── config/             # Settings, CORS
│       ├── controller/         # REST endpoints (/api/empire, /api/icon, /api/settings)
│       ├── dto/                # Response DTOs
│       ├── engine/             # Empire generation & reroll logic
│       ├── extractor/          # Clausewitz game-data extractors (one per entity type)
│       ├── model/              # Domain models (Ethic, Civic, Origin, SpeciesTrait, …)
│       └── parser/             # Clausewitz tokenizer, AST parser, localization
├── frontend/                   # Vite + React 19 app (port 5173)
│   ├── src/
│   │   ├── components/         # UI components (EmpireCard, EntityIcon, …)
│   │   ├── store/              # Zustand state
│   │   └── lib/                # API client, formatting helpers
│   └── src-tauri/              # Tauri Rust shell + config
│       ├── src/lib.rs          # Backend sidecar launcher
│       └── tauri.conf.json     # App config, bundle targets, resources
└── scripts/
    ├── before-build.js         # Cross-platform build orchestration (Node.js)
    ├── bundle-jre.bat          # Windows JRE bundler (jlink)
    └── bundle-jre.sh           # Linux/macOS JRE bundler (jlink)
```

## Backend Logging (production)

In the installed app, backend stdout/stderr are redirected to a log file for diagnostics:

| OS | Log location |
|----|-------------|
| Windows | `%APPDATA%\com.stellaris.bs-generator\logs\backend.log` |
| Linux | `~/.local/share/com.stellaris.bs-generator/logs/backend.log` |
| macOS | `~/Library/Logs/com.stellaris.bs-generator/backend.log` |
