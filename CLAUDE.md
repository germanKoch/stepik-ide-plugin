# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Stepik plugin — a Kotlin/JVM project (part of the ai-sec-lab workspace). Currently in early scaffolding stage with a single entry point at `src/main/kotlin/Main.kt`.

## Build & Test Commands

```bash
./gradlew build          # compile + test
./gradlew test           # run all tests (JUnit 5 via useJUnitPlatform())
./gradlew run            # run Main.kt (requires application plugin — not yet configured)
```

## Tech Stack

- **Language:** Kotlin 2.2, JVM toolchain 24
- **Build:** Gradle 8.14 (Kotlin DSL), foojay toolchain resolver
- **Testing:** kotlin-test with JUnit Platform