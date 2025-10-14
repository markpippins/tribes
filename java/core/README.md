# beatgen-core

beatgen-core is the central library module that contains the domain model, utilities, and core sequencing logic used by the UI and Spring modules.

Key information

- ArtifactId: `beatgen-core`
- Packaging: jar
- Java target: configured to 18 in module POM (parent targets 17). Align as needed.

Important dependencies

- Lombok (provided)
- Jackson XML: `jackson-dataformat-xml`
- Logging: Logback
- Jedis (Redis client)
- Jakarta Persistence API (interfaces)

Build and test

From the repo root:

```powershell
cd java/core
mvn -DskipTests package
mvn test
```

Usage

- This module is a plain Java library and is consumed by the `beatgen-spring` and `beatgen-swing` modules via a project dependency.

Documentation suggestions / next refactors

- Add high-level package documentation: describe `sequencer`, `model`, `service` packages and the key classes (e.g., `MelodicSequencer`, `Song`, `Session`).
- Add a small example main class or unit/integration test that demonstrates constructing a `Session` and running a short sequence. This helps new contributors.
- Add javadocs to public APIs and consider publishing API docs via a site or gh-pages.
