# Next Steps - Phase 2 Refactoring

## Overview

Phase 1 successfully consolidated 14 managers into 7 services. Phase 2 will complete the cleanup by removing deprecated code and further streamlining the service layer.

## Current Status

❌ **Attempted to delete old managers but discovered remaining references**

The following files still reference old managers and need to be updated:
- `Session.java` - References DeviceManager, InstrumentManager, PlayerManager
- `DrumSequencer.java` - References DeviceManager, DrumSequencerManager, InstrumentManager, InternalSynthManager, PlayerManager, ReceiverManager
- `MelodicSequencer.java` - References DeviceManager, MelodicSequencerManager, PlayerManager
- `RuleHelper.java` - References PlayerManager
- `MelodicSequenceDataHelper.java` - References DeviceManager, InstrumentManager, PlayerManager
- `PatternSequencer.java` - References DrumSequencerManager, MelodicSequencerManager

## Phase 2 Goals (Revised)

1. **Update all remaining references** to use new services
2. **Remove deprecated manager classes** (8 files)
3. **Verify build succeeds**
4. **Consider further consolidation** of utility services
5. **Standardize naming conventions** (Manager → Service)

## Step 1: Remove Deprecated Managers

These files are no longer used and can be safely deleted:

### MIDI Management (replaced by MidiService)
- `java/core/src/main/java/com/angrysurfer/core/service/DeviceManager.java`
- `java/core/src/main/java/com/angrysurfer/core/service/ReceiverManager.java`
- `java/core/src/main/java/com/angrysurfer/core/service/InternalSynthManager.java`

### Playback Management (replaced by PlaybackService)
- `java/core/src/main/java/com/angrysurfer/core/service/PlayerManager.java`
- `java/core/src/main/java/com/angrysurfer/core/service/InstrumentManager.java`

### Sequencer Management (replaced by SequencerService)
- `java/core/src/main/java/com/angrysurfer/core/service/DrumSequencerManager.java`
- `java/core/src/main/java/com/angrysurfer/core/service/MelodicSequencerManager.java`

### Soundbank Management (replaced by SoundbankService)
- `java/core/src/main/java/com/angrysurfer/core/service/SoundbankManager.java` (old version)

### Verification Commands

Before deleting, verify no references exist:

```bash
# Search for DeviceManager references
grep -r "DeviceManager" java/core/src --include="*.java"
grep -r "DeviceManager" java/swing/beatsui/src --include="*.java"

# Search for ReceiverManager references
grep -r "ReceiverManager" java/core/src --include="*.java"
grep -r "ReceiverManager" java/swing/beatsui/src --include="*.java"

# Search for InternalSynthManager references
grep -r "InternalSynthManager" java/core/src --include="*.java"
grep -r "InternalSynthManager" java/swing/beatsui/src --include="*.java"

# Search for PlayerManager references (excluding PlaybackService)
grep -r "PlayerManager" java/core/src --include="*.java" | grep -v "PlaybackService"
grep -r "PlayerManager" java/swing/beatsui/src --include="*.java"

# Search for InstrumentManager references (excluding PlaybackService)
grep -r "InstrumentManager" java/core/src --include="*.java" | grep -v "PlaybackService"
grep -r "InstrumentManager" java/swing/beatsui/src --include="*.java"

# Search for DrumSequencerManager references
grep -r "DrumSequencerManager" java/core/src --include="*.java"
grep -r "DrumSequencerManager" java/swing/beatsui/src --include="*.java"

# Search for MelodicSequencerManager references
grep -r "MelodicSequencerManager" java/core/src --include="*.java"
grep -r "MelodicSequencerManager" java/swing/beatsui/src --include="*.java"

# Search for old SoundbankManager references
grep -r "SoundbankManager" java/core/src --include="*.java" | grep -v "SoundbankService"
grep -r "SoundbankManager" java/swing/beatsui/src --include="*.java"
```

## Step 2: Review Remaining Services

After cleanup, the service package will contain:

### Core Services (Keep)
- ✅ `MidiService.java` - MIDI device and synthesizer management
- ✅ `PlaybackService.java` - Player and instrument management
- ✅ `SequencerService.java` - Sequencer management
- ✅ `SoundbankService.java` - Soundbank management
- ✅ `SessionManager.java` - Session state management
- ✅ `UserConfigManager.java` - User configuration
- ✅ `ChannelManager.java` - MIDI channel allocation

### Utility Services (Review)
- ❓ `AudioFileManager.java` - Audio file operations
- ❓ `LogManager.java` - Logging configuration
- ❓ `SamplePlaybackService.java` - Sample playback

### Questions to Answer:
1. Can AudioFileManager be simplified or merged?
2. Is LogManager necessary or can it be replaced with static configuration?
3. Should SamplePlaybackService be renamed or consolidated?

## Step 3: Naming Consistency

Consider renaming for consistency:

### Current Naming
- Services: MidiService, PlaybackService, SequencerService, SoundbankService
- Managers: SessionManager, UserConfigManager, ChannelManager, AudioFileManager, LogManager

### Proposed Naming (Option A - All Services)
- MidiService ✓
- PlaybackService ✓
- SequencerService ✓
- SoundbankService ✓
- SessionService (rename from SessionManager)
- UserConfigService (rename from UserConfigManager)
- ChannelService (rename from ChannelManager)
- AudioFileService (rename from AudioFileManager)
- LogService (rename from LogManager)

### Proposed Naming (Option B - Keep Managers for State)
- Services: MidiService, PlaybackService, SequencerService, SoundbankService
- Managers: SessionManager, UserConfigManager, ChannelManager (keep for state management)
- Utilities: AudioFileUtil, LogUtil (rename to utilities)

## Step 4: Code Quality Improvements

### Fix Warnings in New Services

**PlaybackService.java:**
- Remove unused import: `com.angrysurfer.core.model.Session`

**SequencerService.java:**
- Remove unused import: `javax.sound.midi.Synthesizer`

**App.java:**
- Remove unused imports: `List`, `InstrumentWrapper`, `InstrumentHelper`
- Remove unused field: `commandBus`
- Remove unused variable: `redisService`
- Fix constant naming: `showSplash` → `SHOW_SPLASH`

### Improve Documentation

Add JavaDoc to new services:
- Class-level documentation
- Method-level documentation for public APIs
- Parameter descriptions
- Return value descriptions
- Exception documentation

## Step 5: Testing Strategy

### Unit Tests
Create unit tests for new services:
- `MidiServiceTest.java`
- `PlaybackServiceTest.java`
- `SequencerServiceTest.java`
- `SoundbankServiceTest.java`

### Integration Tests
Test service interactions:
- MIDI device initialization
- Player/instrument assignment
- Preset changes
- Sequencer playback

### Manual Testing Checklist
- [ ] Application starts successfully
- [ ] MIDI devices are discovered
- [ ] Internal synthesizer works
- [ ] External MIDI devices work
- [ ] Drum sequencer plays correctly
- [ ] Melodic sequencer plays correctly
- [ ] Preset changes apply correctly
- [ ] Session save/load works
- [ ] Pattern switching works
- [ ] Channel allocation works
- [ ] MIDI repair works after device disconnect

## Step 6: Performance Validation

### Metrics to Track
- Application startup time
- MIDI latency
- Memory usage
- CPU usage during playback
- Redis operation latency

### Benchmarks
- Startup time: < 3 seconds
- MIDI latency: < 10ms
- Memory usage: < 500MB
- CPU usage: < 20% during playback

## Step 7: Documentation Updates

### Update After Cleanup
- [x] ARCHITECTURE.md - Created
- [x] REFACTORING_SUMMARY.md - Updated
- [x] README.md - Updated
- [ ] Add JavaDoc to new services
- [ ] Update inline comments
- [ ] Create API documentation

### New Documentation Needed
- [ ] Developer Guide
- [ ] API Reference
- [ ] Testing Guide
- [ ] Deployment Guide

## Timeline Estimate

- **Step 1 (Remove deprecated):** 1 hour
- **Step 2 (Review services):** 2 hours
- **Step 3 (Naming consistency):** 2 hours
- **Step 4 (Code quality):** 3 hours
- **Step 5 (Testing):** 8 hours
- **Step 6 (Performance):** 4 hours
- **Step 7 (Documentation):** 4 hours

**Total:** ~24 hours (3 days)

## Success Criteria

- ✅ All deprecated managers removed
- ✅ No compilation errors
- ✅ All tests passing
- ✅ No performance regression
- ✅ Documentation updated
- ✅ Code quality improved
- ✅ Naming consistent
- ✅ Manual testing complete

## Risks and Mitigation

### Risk: Hidden dependencies on old managers
**Mitigation:** Comprehensive grep search before deletion

### Risk: Performance regression
**Mitigation:** Benchmark before and after, profile if needed

### Risk: Breaking changes in UI
**Mitigation:** Thorough manual testing of all UI features

### Risk: Redis data compatibility
**Mitigation:** Verify data structures unchanged, test save/load

## Rollback Plan

If issues are discovered:
1. Revert to previous commit
2. Identify specific issue
3. Fix in isolation
4. Re-apply changes incrementally

## Next Phase (Phase 3)

After Phase 2 completion, consider:
- MIDI file import/export
- VST plugin support
- Automation lanes
- MIDI learn functionality
- External MIDI clock sync
- Multi-track recording
- Undo/redo system
- Improved visualization
