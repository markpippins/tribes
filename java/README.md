BeatGeneratorApp Java Modules

This folder contains the multi-module Java project for BeatGeneratorApp. The project is organized as a parent POM with three child modules:

- `beatgen-core` (core library)
- `beatgen-spring` (Spring Boot web/service layer)
- `beatgen-swing` (Swing-based UI)

Purpose

This parent module coordinates dependency management (via Spring Boot BOM) and common properties for the child modules. It centralizes versions for Spring Boot, Spring Framework, Jackson, junit, etc.

Quick start (from repository root)

1. Build all modules (skip tests for faster local iteration):

```powershell
cd java
mvn -DskipTests package
```

2. Build and run tests:

```powershell
cd java
mvn test
```

Module-level commands

- Build a single module (example `core`):

```powershell
cd java\core
mvn -DskipTests package
```

Notes

- Java version: configured for Java 17 in parent POM, child modules include a `java.version` property of 18 in some POMs. Ensure local JDK matches the configured target (17 or 18) or align the POMs.
- Spring Boot version managed in parent via `${spring.boot.version}`. Changing it updates child modules that use the BOM.

Next steps / refactor suggestions

- Normalize `java.version` across parent and modules (use a single source of truth).
- Add module-level READMEs (done) and include module badges and CI instructions when CI is available.
- Consider adding a Maven `release` or `versions` plugin configuration for smoother upgrades of Spring Boot.
