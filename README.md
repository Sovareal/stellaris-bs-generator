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
| Backend | Java 25 (Corretto), Spring Boot 4.0, Gradle |
| Frontend | React 19, TypeScript, Tailwind CSS |
| Desktop Shell | Tauri 2.0 |

## Building & Running

### Backend
```bash
gradle :backend:bootJar      # Build JAR
gradle :backend:bootRun       # Run dev server on :8080
gradle :backend:test          # Run tests
```

### Frontend
```bash
cd frontend
npm install                   # Install dependencies
npm run dev                   # Dev mode (UI only, :5173)
npm run tauri:dev             # Full app (Tauri + backend sidecar)
npm run tauri:build           # Production build
```

### Prerequisites
- Java 25 (Amazon Corretto)
- Node.js 24+
- Rust (for Tauri)
- Stellaris installed locally
