# Sound System Refactoring Proposal

## Current Issues

After analyzing the codebase, I've identified several overlapping responsibilities and redundancies between the core manager classes:

1. **Duplicate Synthesizer Management**:
   - Both `SoundbankManager` and `InternalSynthManager` were independently creating and managing synthesizer instances
   - This caused state inconsistencies when changing soundbanks and presets

2. **Unclear Responsibilities**:
   - `InternalSynthManager`: Manages internal synthesizer but has some soundbank-related functionality
   - `SoundbankManager`: Manages soundbanks but duplicates synthesizer management
   - `InstrumentManager`: Manages instruments with some overlap in functionality

3. **Redundant Code**:
   - Duplicate methods for initializing synthesizers
   - Similar methods for playing notes and applying presets

## Proposed Solution

### Short-term fixes (already implemented)

1. **Use a single synthesizer instance**:
   - Modified `SoundbankManager` to use `InternalSynthManager`'s synthesizer instance
   - Removed duplicate synthesizer initialization
   - Improved logging to track soundbank changes

### Long-term Refactoring Plan

1. **Clear Separation of Responsibilities**:

   - **SynthesizerService**: 
     - Single source of truth for synthesizer access
     - Manages synthesizer lifecycle (open/close)
     - Provides raw MIDI channel access

   - **SoundbankService**:
     - Pure soundbank management (loading, unloading)
     - No direct synthesizer ownership
     - Depends on SynthesizerService

   - **InstrumentService**:
     - Manages instrument configurations
     - Applies instrument settings to channels via SynthesizerService
     - Coordinates with SoundbankService for soundbank application

2. **Simplified API Design**:

```java
// SynthesizerService (core low-level service)
public class SynthesizerService {
    private static SynthesizerService instance;
    private Synthesizer synthesizer;
    
    public static SynthesizerService getInstance() { /* ... */ }
    public Synthesizer getSynthesizer() { /* ... */ }
    public void ensureOpen() { /* ... */ }
    public void playNote(int note, int velocity, int duration, int channel) { /* ... */ }
}

// SoundbankService (depends on SynthesizerService)
public class SoundbankService {
    private static SoundbankService instance;
    private final Map<String, Soundbank> soundbanks = new LinkedHashMap<>();
    
    public List<String> getSoundbankNames() { /* ... */ }
    public boolean loadSoundbank(File file) { /* ... */ }
    public boolean applySoundbank(String soundbankName) { /* ... */ }
    // No synthesizer management
}

// InstrumentService (coordinates both services)
public class InstrumentService {
    private static InstrumentService instance;
    
    public void applyInstrumentSettings(InstrumentWrapper instrument, int channel) { /* ... */ }
    public void applyPreset(InstrumentWrapper instrument, Integer bankIndex, Integer preset) { /* ... */ }
    // Delegates to SoundbankService and SynthesizerService
}
```

3. **Migration Path**:
   - Implement new services alongside existing managers
   - Gradually migrate functionality
   - Update consumers to use new APIs
   - Remove old managers once migration is complete

## Benefits

1. **Improved Reliability**: Single source of truth for synthesizer instance
2. **Clearer Code Organization**: Well-defined responsibilities with minimal overlap
3. **Simplified Debugging**: Easier to trace issues when responsibilities are clearly separated
4. **Better Maintainability**: Smaller, focused classes with single responsibilities
5. **Reduced Coupling**: Services depend on abstractions rather than concrete implementations

## Implementation Timeline

1. Phase 1: Implement the core SynthesizerService (2 days)
2. Phase 2: Refactor SoundbankService to use SynthesizerService (3 days)
3. Phase 3: Create InstrumentService with coordinating functionality (3 days)
4. Phase 4: Migrate consumers to new APIs (5-7 days)
5. Phase 5: Remove deprecated managers and code cleanup (2 days)

Total estimated time: 15-17 days
