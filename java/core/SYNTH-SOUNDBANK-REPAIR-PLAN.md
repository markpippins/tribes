# Synth / Soundbank Repair Plan

This document collects the full analysis, critique and phased repair plan for the MIDI, synthesizer and soundbank plumbing in BeatGeneratorApp. It is intended for sharing with the team and for use as the basis of the refactor.

> Source notes: this plan references the short-term fixes in `SOUNDBANK-FIX.md` and the proposed architecture in `REFACTORING-PROPOSAL.md`.

---

## Executive summary

- Symptoms: application hang at startup (circular initialization), inconsistent synthesizer state, redundant operations when applying soundbanks/presets, fragile implicit initialization ordering, and poor testability.
- Immediate fix: ensure explicit, deterministic startup ordering (open synth -> initialize synth data -> load soundbanks -> start consumers) and remove cross-manager constructor calls. This was implemented in `SOUNDBANK-FIX.md`.
- Long-term solution: implement three focused services:
  - `SynthesizerService` — owns and manages the Synthesizer lifecycle.
  - `SoundbankService` — loads and indexes soundbanks; applies soundbanks to the synth via SynthesizerService.
  - `InstrumentService` — coordinates preset/program application using the two services above.
- Migration: incremental and adapter-based to avoid big-bang changes; keep legacy managers as thin adapters during migration.

---

## Current component map (observed)

- `App` (bootstrap)
- `InternalSynthManager` — currently opens or manages a Synthesizer instance and offers synth access
- `SoundbankManager` — loads/buffers soundbanks and applies them. Historically duplicated some synth management.
- `InstrumentManager` / `PlayerManager` — apply presets and play notes; sometimes re-applies soundbanks.
- Spring/Web controllers and UI (Swing) consumers call into the managers.

See `SOUNDBANK-FIX.md` and `REFACTORING-PROPOSAL.md` for details.

---

## Problems & evidence

1. Circular dependencies and implicit initialization
   - Evidence: `SOUNDBANK-FIX.md` described cross-initialization between `InternalSynthManager` and `SoundbankManager` causing startup hangs.
   - Risk: deadlocks, infinite loops, non-deterministic start failures.

2. Multiple owners of Synthesizer
   - Evidence: `REFACTORING-PROPOSAL.md` noted both managers created/manipulated the synthesizer.
   - Risk: state divergence and double-applying soundbanks causing wrong instrument sounds.

3. Hidden side-effects in constructors
   - Evidence: constructor-level init calls found in legacy manager patterns.
   - Risk: initialization order sensitivity; small refactors break startup.

4. Duplicate application of soundbanks/presets
   - Evidence: `SOUNDBANK-FIX.md` reported duplicate soundbank application in PlayerManager.
   - Risk: redundant CPU/native calls, incorrect instrumentation, performance issues.

5. Concurrency and resource safety
   - Evidence: no explicit concurrency model for Synthesizer operations described; javax.sound.midi is native and requires careful lifecycle handling.
   - Risk: races, illegal state exceptions, native crashes.

6. Poor testability
   - Evidence: direct usage of concrete Synthesizer and managers, no clear abstraction to mock.
   - Risk: audio code cannot be reliably unit tested in CI.

7. Weak error handling and monitoring
   - Evidence: limited guidance on how to continue when soundbank fails or synth is absent.
   - Risk: whole app may fail or hang in degraded environments.

---

## Design contract (service responsibilities)

### SynthesizerService
- Single owner of Synthesizer. Responsible for opening, closing, and basic channel/program operations.
- Ensure thread-safety (internal lock) and idempotence of open/close.
- Expose clear checked exceptions for failure modes.

Core methods (proposal):
```java
void ensureOpen() throws SynthException;
boolean isOpen();
ISynthesizer getSynthWrapper();
void setProgram(int channel, int bank, int program) throws SynthException;
void playNote(int channel, int note, int velocity, Duration duration) throws SynthException;
void close();
```

### SoundbankService
- Pure soundbank management: load, unload, list, and apply. Does not own synth lifecycle.
- Applies soundbank to synth by delegating to SynthesizerService.

Core methods:
```java
List<String> listSoundbanks();
boolean loadSoundbank(File f) throws SoundbankException;
boolean applySoundbank(String name) throws SoundbankException;
Optional<Soundbank> getSoundbank(String name);
```

### InstrumentService
- Coordinates application of instrument/preset changes.
- Delegates bank selection to SoundbankService and program/patch changes to SynthesizerService.
- Ensures operations are atomic and idempotent.

Core methods:
```java
void applyPreset(InstrumentDescriptor instr, Optional<Integer> bank, Optional<Integer> preset);
void applyInstrumentSettings(InstrumentDescriptor instr, int channel);
```

---

## Phased repair & migration plan (detailed)

### Phase 0 — Stabilize startup (immediate)
- Ensure managers do not call each other in constructors.
- Provide explicit initialization methods and centralize startup sequence in `App`:
  1. SynthesizerService.ensureOpen()
  2. SynthesizerService.initializeSynthData() (if needed)
  3. SoundbankManager.initializeSoundbanks()
  4. InstrumentManager / PlayerManager startup
- Add startup logs and short timeouts for hangs.
- Acceptance: app starts deterministically in dev environment.
- Estimate: 1 day.

### Phase 1 — Introduce SynthesizerService + adapters (non-breaking)
- Create `ISynthesizer` wrapper and `SynthesizerService` that encapsulates javax.sound.midi.Synthesizer usage.
- Make `InternalSynthManager` use `SynthesizerService` internally (delegation). No external API changes.
- Add unit tests mocking ISynthesizer.
- Add health check exposing synth state.
- Acceptance: single open/close owner; tests for open/close and program application.
- Estimate: 2–3 days.

### Phase 2 — Implement SoundbankService & InstrumentService
- Implement `SoundbankService` and `InstrumentService` with unit tests.
- Replace heavy logic in legacy managers with thin delegators to new services.
- Add integration test(s) with a software synth or controlled fake.
- Acceptance: soundbank loads correctly and `applySoundbank` uses SynthesizerService; instrument changes are atomic.
- Estimate: 3–5 days.

### Phase 3 — Migrate consumers
- Replace direct synth/bank/preset calls in controllers, PlayerManager, tests.
- Run smoke tests and compare audio behavior to legacy path.
- Add feature-flag config for progressive rollout if needed.
- Acceptance: all call sites use services; legacy managers only delegate or are unused.
- Estimate: 2–4 days.

### Phase 4 — Clean up
- Remove legacy managers, finalize docs and diagrams.
- Acceptance: clean build, tests pass, docs updated.
- Estimate: 1–2 days.

---

## Testing plan

### Unit tests
- Create mocks/fakes around ISynthesizer and test SynthesizerService behavior: open/close, setProgram, playNote, failure/retry.
- Test SoundbankService: load valid/invalid banks, apply bank, idempotency.
- Test InstrumentService: applyPreset atomicity and idempotency.

### Integration tests (optional on CI)
- Software synth harness (Gervill) or headless stub that accepts MIDI messages.
- Test full flow: load bank -> apply preset -> play notes; validate no exceptions and expected operations occurred.

### Health & metrics
- Expose /actuator/health (if Spring) with SynthesizerService status.
- Counters for bank loads, bank load failures, preset apply operations, and last error message.

---

## Concurrency & lifecycle rules

- All synth-modifying operations must call `ensureOpen()` first.
- Use a single ReentrantLock around patch/program and bank apply operations to make them atomic.
- Read-only queries can avoid locks but must handle the synth being closed concurrently.
- Graceful close: on shutdown, reject new operations, wait for inflight ops to complete, then close synthesizer.

---

## Rollout & rollback guidance

- Feature-flag the new services behind a configuration flag (e.g., `audio.useNewServices=true`). Start with flag OFF in production.
- Smoke-test in dev with flag ON; if stable, enable in staging; finally enable in production.
- Rollback is switching the flag back to OFF (legacy managers continue to work during migration).

---

## Developer checklist (actionable)
- [ ] Add `ISynthesizer` interface.
- [ ] Implement `SynthesizerService` with unit tests.
- [ ] Implement `SoundbankService` with unit tests.
- [ ] Implement `InstrumentService` with unit tests.
- [ ] Replace logic in `InternalSynthManager` and `SoundbankManager` to delegate to services.
- [ ] Add integration tests and actuator health endpoint.
- [ ] Deploy behind a feature flag and run smoke tests across environments.

---

## Acceptance criteria (summary)
- Deterministic startup (no hangs). ✅
- Single Synthesizer owner. ✅
- Dedicated services for bank/instrument operations; legacy managers removed or thinned. ✅
- Tests covering the major flows. ✅
- Health endpoint and logs to diagnose issues. ✅

---

## Appendix: short-term quick wins

1. Add logging at the start and end of each manager initialization.
2. Add timeouts to startup steps for faster failure detection.
3. Ensure constructors do not have side-effects.
4. Add a lightweight ISynthesizer mock for unit tests so CI can run quickly.

---

If you want, I can now:
- Generate a code sketch for `ISynthesizer` and `SynthesizerService` (no production change),
- Create unit test skeletons (JUnit + Mockito),
- Or open a feature branch and implement the minimal SynthesizerService and adapters behind a feature flag and push a PR.

Which of these would you like next?