#!/usr/bin/env node
/**
 * Cross-platform before-build script for Tauri.
 * Replaces the Windows-only `beforeBuildCommand` chain in tauri.conf.json.
 * Runs on Windows, Linux, and macOS.
 */
'use strict';

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const isWin = process.platform === 'win32';

function run(cmd, cwd) {
    console.log(`\n> ${cmd}`);
    execSync(cmd, { cwd: cwd ?? root, stdio: 'inherit', shell: true });
}

// 1. Build the Spring Boot backend JAR
const gradlew = isWin ? 'gradlew.bat' : './gradlew';
run(`${gradlew} :backend:bootJar`);

// 2. Copy the JAR into src-tauri so Tauri can bundle it as a resource
const gradleContent = fs.readFileSync(path.join(root, 'backend', 'build.gradle.kts'), 'utf8');
const versionMatch = gradleContent.match(/^version\s*=\s*"([^"]+)"/m);
const backendVersion = versionMatch ? versionMatch[1] : '0.1.0';
const jarSrc = path.join(root, 'backend', 'build', 'libs', `backend-${backendVersion}.jar`);
const jarDst = path.join(root, 'frontend', 'src-tauri', 'backend.jar');
console.log(`\nCopying ${jarSrc} → ${jarDst}`);
fs.copyFileSync(jarSrc, jarDst);

// 3. Bundle a minimal JRE using jlink
const bundleScript = isWin
    ? path.join(__dirname, 'bundle-jre.bat')
    : path.join(__dirname, 'bundle-jre.sh');
const bundleCmd = isWin ? `"${bundleScript}"` : `sh "${bundleScript}"`;
run(bundleCmd);

// 4. Build the Vite + TypeScript frontend
run('npm run build', path.join(root, 'frontend'));

console.log('\nBefore-build complete.');
