# Stellaris BS Empire Generator

A desktop application that generates random, rule-valid Stellaris empires by parsing the game's own data files.

## How It Works

The app reads your local Stellaris installation files (Clausewitz `.txt` format) and extracts all ethics, authorities, civics, origins, species archetypes, and traits — along with their compatibility rules. It then generates randomized empires that respect every constraint the game enforces.

Each generated empire includes:
- **Ethics** (cost-balanced to 3 points)
- **Authority** (compatible with chosen ethics)
- **2 Civics** (respecting mutual exclusions and prerequisites)
- **Origin** (valid for the selected government)
- **Species** with traits (within point budget, no conflicting opposites)

You get one reroll per category if you don't like a pick.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 25 (Corretto), Spring Boot 4.0, Gradle, Lombok |
| Frontend | React 19, TypeScript, Tailwind CSS |
| Desktop Shell | Tauri 2.0 |

## Building & Running

### Prerequisites
- Java 25 (Amazon Corretto) — needed for building; bundled in the installer for end users
- Node.js 24+
- Rust (for Tauri)
- Stellaris installed locally

### Development

```bash
# Backend
gradlew.bat :backend:bootJar      # Build JAR
gradlew.bat :backend:bootRun      # Run dev server on :8080
gradlew.bat :backend:test         # Run tests

# Frontend
cd frontend
npm install                        # Install dependencies (first time)
npm run dev                        # Dev mode (UI only, :5173)
npm run tauri dev                  # Full app (Tauri + backend sidecar)
```

### Production Build

Build the NSIS installer (Windows):

```bash
cd frontend
npm run tauri build
```

This runs the full pipeline automatically:
1. `gradlew.bat :backend:bootJar` — compiles the Spring Boot backend into a fat JAR
2. Copies `backend.jar` into `src-tauri/`
3. `scripts/bundle-jre.bat` — uses `jlink` to create a minimal JRE (~50 MB) with only the modules Spring Boot needs
4. `npm run build` — builds the React frontend
5. Compiles the Tauri Rust shell in release mode
6. Packages everything into an NSIS installer

The installer is output to:
```
frontend/src-tauri/target/release/bundle/nsis/Stellaris BS Generator_0.1.0_x64-setup.exe
```

### Running the Installed App

The installer bundles a minimal JRE — no system Java installation required. On launch, the Tauri shell starts the Spring Boot backend using the bundled JRE and opens the UI. The app will prompt you to set your Stellaris game path on first run if it can't find the default location.
