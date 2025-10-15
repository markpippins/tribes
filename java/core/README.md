Phase 0 — Startup Hardening

Purpose

This README documents the "Phase‑0" startup hardening pattern used in the BeatGeneratorApp core module.

Goals

- Make constructors side‑effect‑free so object construction is deterministic and low-cost.
- Move external side-effects (I/O, network access, device initialization, heavy computation, registrations) into an explicit public synchronized initialize() method.
- Ensure a deterministic startup ordering by calling initialize() on components in a controlled sequence.
- Replace console prints (System.out/System.err/printStackTrace) with SLF4J logging for consistent, configurable logging and to avoid spurious console output during headless CI runs.

Contract

- Constructor:
	- Must only set field defaults and prepare internal data structures.
	- Must not perform I/O, blocking operations, logging of side-effects that would alter global state, device access, or event bus registration.
	- Must not start background threads.

- initialize():
	- Public, synchronized, idempotent — calling initialize() multiple times should be safe (no duplicate registrations or repeated heavy work).
	- Performs side-effects necessary to bring the component online (e.g., connect to Redis, open MIDI devices, register listeners on the CommandBus/TimingBus, load soundbanks).
	- Returns quickly when possible; if it must block, prefer explicit timeout handling and clear logging.
	- Log at appropriate levels (info for high-level progress, debug/trace for detailed internals, warn/error for problems).

Startup Ordering (recommended)

1. Infrastructure (RedisService, configuration, environment validation)
2. Device managers (DeviceManager — MIDI devices)
3. Soundbanks and synthesizer initialization (SoundbankManager, InternalSynthManager)
4. Domain managers that rely on devices/data (InstrumentManager, PlayerManager)
5. Session and runtime orchestration (SessionManager, CommandBus publishers/listeners)
6. UI initialization (create frames/panels) — UI should only be started after backend services are initialized

Developer checklist

- When making a class that previously performed side-effects in the constructor:
	- Add a public synchronized void initialize() method and move the side-effects there.
	- Add a private volatile boolean initialized flag or equivalent to make initialize() idempotent.
	- Ensure unit tests cover the initialize contract (constructors are side-effect-free; initialize performs expected registration).
- Replace System.out/System.err/printStackTrace with SLF4J:
	- Use org.slf4j.Logger and LoggerFactory.getLogger(Class.class).
	- Pass exceptions to logger methods (e.g., logger.error("Failed to open device", e)).
- For UI code that shows dialogs on exceptions, keep the dialog behavior but log the underlying stack trace with SLF4J as well.

Notes

- This pattern reduces flakiness in tests and improves control over startup ordering.
- When in doubt, prefer explicit initialize() methods rather than putting logic into constructors.

Examples

- See `com.angrysurfer.core.service.InternalSynthManager` (init sequence) and `com.angrysurfer.beats.App` (explicit background initialization) for applied patterns.

"Phase‑1" follow-ups (suggested)

- Add automated static checks to detect System.out/System.err/printStackTrace usages in CI.
- Add tests that construct objects in a headless/isolated environment to assert constructors do not perform side-effects.

---

````markdown
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

````
