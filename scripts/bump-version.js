#!/usr/bin/env node
/**
 * Sync version across all 4 manifest files.
 * Usage: node scripts/bump-version.js <version>
 * Example: node scripts/bump-version.js 1.2.3
 */
'use strict';

const fs = require('fs');
const path = require('path');

const version = process.argv[2];

if (!version || !/^\d+\.\d+\.\d+$/.test(version)) {
    console.error('Usage: node scripts/bump-version.js <major.minor.patch>');
    process.exit(1);
}

const root = path.resolve(__dirname, '..');

// 1. frontend/src-tauri/tauri.conf.json
const tauriConfPath = path.join(root, 'frontend', 'src-tauri', 'tauri.conf.json');
const tauriConf = JSON.parse(fs.readFileSync(tauriConfPath, 'utf8'));
tauriConf.version = version;
fs.writeFileSync(tauriConfPath, JSON.stringify(tauriConf, null, 2) + '\n');
console.log(`Updated tauri.conf.json -> ${version}`);

// 2. frontend/package.json
const pkgPath = path.join(root, 'frontend', 'package.json');
const pkg = JSON.parse(fs.readFileSync(pkgPath, 'utf8'));
pkg.version = version;
fs.writeFileSync(pkgPath, JSON.stringify(pkg, null, 2) + '\n');
console.log(`Updated frontend/package.json -> ${version}`);

// 3. backend/build.gradle.kts
const gradlePath = path.join(root, 'backend', 'build.gradle.kts');
let gradleContent = fs.readFileSync(gradlePath, 'utf8');
gradleContent = gradleContent.replace(/^version\s*=\s*"[^"]+"/m, `version = "${version}"`);
fs.writeFileSync(gradlePath, gradleContent);
console.log(`Updated backend/build.gradle.kts -> ${version}`);

// 4. frontend/src-tauri/Cargo.toml
const cargoPath = path.join(root, 'frontend', 'src-tauri', 'Cargo.toml');
let cargoContent = fs.readFileSync(cargoPath, 'utf8');
// Replace only the version in the [package] section (first occurrence)
cargoContent = cargoContent.replace(/^(version\s*=\s*)"[^"]+"/m, `$1"${version}"`);
fs.writeFileSync(cargoPath, cargoContent);
console.log(`Updated frontend/src-tauri/Cargo.toml -> ${version}`);

console.log(`\nVersion bumped to ${version}.`);
console.log('\nNext steps:');
console.log(`  1. Update CHANGELOG.md with the new release entry`);
console.log(`  2. git add -A && git commit -m "chore: release v${version}"`);
console.log(`  3. git tag v${version}`);
console.log(`  4. git push && git push --tags`);
