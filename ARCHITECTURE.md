# MIDI Sequencer Architecture

## Overview

This is a Java-based MIDI sequencer application with a Swing UI, Redis-backed state management, and real-time MIDI playback capabilities. The architecture follows a service-oriented design with event-driven communication.

## Technology Stack

- **Language:** Java 17/18
- **Build Tool:** Maven (multi-module project)
- **UI Framework:** Swing with FlatLaf Look and Feel
- **State Management:** Redis
- **MIDI:** Java Sound API (javax.sound.midi)
- **Logging:** SLF4J with Logback

## Project Structure

```
java/
├── core/                    # Core business logic and services
│   ├── api/                # Command bus and event system
│   ├── config/             # Configuration models
│   ├── event/              # Event definitions
│   ├── model/              # Domain models
│   ├── redis/              # Redis persistence layer
│   ├── sequencer/          # Sequencer implementations
│   ├── service/            # Business services
│   └── util/               # Utilities and helpers
├── swing/beatsui/          # Swing-based UI application
│   ├── diagnostic/         # Diagnostic tools
│   ├── panel/              # UI panels
│   ├── util/               # UI utilities
│   ├── visualization/      # Visualization components
│   └── widget/             # Custom UI widgets
└── pom.xml                 # Parent POM
```

## Core Architecture

### Service Layer

The service layer provides the main business logic and is organized into focused, single-responsibility services:

#### MIDI Services

**MidiService** - Unified MIDI device and synthesizer management
- Device discovery and management
- Receiver lifecycle management
- Internal synthesizer control
- Preset application to MIDI channels
- Note on/off operations

**SoundbankService** - Soundbank management
- Soundbank loading and caching
- Preset lookup and application
- Instrument preset management

**PlaybackService** - Player and instrument management
- Player lifecycle management
- Instrument assignment and configuration
- Preset changes and updates
- Channel consistency enforcement
- Rule-based playback logic

**SequencerService** - Sequencer management
- Drum sequencer management
- Melodic sequencer management
- MIDI connection repair
- Tempo synchronization

#### State Management Services

**SessionManager** - Session state management
- Session creation and persistence
- Pattern management
- Song structure management

**UserConfigManager** - User configuration
- User preferences
- Default player/instrument settings
- UI state persistence

**ChannelManager** - MIDI channel allocation
- Channel reservation
- Melodic channel allocation
- Drum channel (channel 10) management

#### Utility Services

**AudioFileManager** - Audio file operations
**LogManager** - Logging configuration
**SamplePlaybackService** - Sample playback

### Event System

The application uses a command bus pattern for decoupled communication:

**CommandBus** - Central event dispatcher
- Publish/subscribe pattern
- Type-safe command routing
- Asynchronous event delivery

**Key Events:**
- `PLAYER_UPDATE_EVENT` - Player state changes
- `PLAYER_PRESET_CHANGE_EVENT` - Preset changes
- `PLAYER_INSTRUMENT_CHANGE_EVENT` - Instrument changes
- `DRUM_STEP_UPDATE_EVENT` - Drum sequencer updates
- `MELODIC_SEQUENCER_EVENT` - Melodic sequencer updates
- `PATTERN_SWITCH_EVENT` - Pattern changes

### Data Layer

**RedisService** - Redis connection and operations
- Connection management
- Serialization/deserialization
- Key management

**Helper Classes:**
- `PlayerHelper` - Player persistence
- `InstrumentHelper` - Instrument persistence
- `SessionHelper` - Session persistence
- `PatternHelper` - Pattern persistence
- `DrumSequenceDataHelper` - Drum sequence persistence
- `MelodicSequenceDataHelper` - Melodic sequence persistence

### Domain Models

**Core Models:**
- `Player` - Playback entity with instrument and channel
- `InstrumentWrapper` - MIDI instrument abstraction
- `Session` - Collection of patterns and songs
- `Pattern` - Arrangement of sequencers
- `Song` - Sequence of patterns

**Sequencer Models:**
- `DrumSequencer` - Step-based drum sequencer
- `MelodicSequencer` - Melodic note sequencer
- `DrumSequenceData` - Drum sequence state
- `MelodicSequenceData` - Melodic sequence state
- `Step` - Individual sequencer step
- `Strike` - Note event with velocity and timing

**Configuration Models:**
- `UserConfig` - User preferences
- `FrameState` - UI window state
- `TableState` - Table view state

## UI Architecture

### Main Components

**App** - Application entry point
- Service initialization
- Theme management
- Splash screen coordination

**Frame** - Main application window
- Menu bar
- Tool bar
- Status bar
- Panel management

**Panels:**
- `MainPanel` - Primary workspace
- `TransportPanel` - Playback controls
- `MixerPanel` - Channel mixing
- `GridPanel` - Step sequencer grid
- `LaunchPanel` - Pattern launcher
- `SystemsPanel` - System settings

### UI Patterns

**Dialogs:**
- `DialogManager` - Dialog lifecycle management
- `Dialog` - Base dialog class

**Widgets:**
- `Dial` - Rotary control
- `GridButton` - Step sequencer button
- `DrumButton` - Drum pad button
- `ToggleSwitch` - Toggle control
- `VuMeter` - Level meter

**Visualization:**
- `Visualizer` - Main visualization engine
- `IVisualizationHandler` - Visualization strategy interface
- Font-based rendering system

## Data Flow

### Initialization Flow

```
1. App.main()
2. Show SplashScreen
3. Initialize Services (background thread)
   - RedisService
   - MidiService
   - SoundbankService
   - PlaybackService
   - UserConfigManager
   - SessionManager
4. Setup Look and Feel
5. Create Frame
6. Show UI
```

### Playback Flow

```
1. User triggers playback
2. TransportPanel publishes command
3. Sequencer receives command
4. Sequencer generates note events
5. Player receives note events
6. InstrumentWrapper sends MIDI
7. MidiService routes to device/synthesizer
8. Audio output
```

### State Persistence Flow

```
1. User modifies state
2. Service updates model
3. Event published to CommandBus
4. RedisService persists to Redis
5. UI components receive update event
6. UI refreshes
```

## Key Design Patterns

### Singleton Pattern
Services use singleton pattern for global access:
- `MidiService.getInstance()`
- `PlaybackService.getInstance()`
- `RedisService.getInstance()`

### Observer Pattern
CommandBus implements publish/subscribe:
- Services register for specific commands
- Publishers fire events
- Subscribers receive notifications

### Strategy Pattern
Visualization system uses strategy pattern:
- `IVisualizationHandler` interface
- Multiple handler implementations
- Runtime handler selection

### Factory Pattern
Sequencer creation:
- `SequencerService.createDrumSequencer()`
- `SequencerService.createMelodicSequencer()`

## Threading Model

### EDT (Event Dispatch Thread)
- All UI operations on EDT
- SwingUtilities.invokeLater() for async UI updates

### Background Threads
- Service initialization
- Redis operations
- MIDI clock (LowLatencyMidiClock)
- Timer-based sequencing

### Thread Safety
- ConcurrentHashMap for caches
- Synchronized methods for critical sections
- Immutable models where possible

## Configuration

### Redis Configuration
- Default: localhost:6379
- Configurable via RedisConfig
- Connection pooling

### MIDI Configuration
- Auto-discovery of MIDI devices
- Preference for Gervill synthesizer
- Fallback to system default

### UI Configuration
- Persisted frame state
- Theme selection
- Panel layouts

## Error Handling

### Initialization Errors
- Graceful degradation
- User notification dialogs
- Application exit on critical failures

### Runtime Errors
- UIErrorHandler for UI errors
- ErrorHandler for service errors
- Logging at appropriate levels

### MIDI Errors
- Connection repair mechanism
- Fallback to internal synthesizer
- User notification of device issues

## Performance Considerations

### Caching
- Device cache in MidiService
- Receiver cache in MidiService
- Soundbank cache in SoundbankService
- Player/instrument caches in PlaybackService

### Lazy Initialization
- Services initialized on demand
- UI panels created lazily
- Soundbanks loaded on first use

### Resource Management
- Proper cleanup in shutdown hooks
- Receiver closing
- Device closing
- Redis connection management

## Future Improvements

### Phase 2 Refactoring
- Remove deprecated manager classes
- Consolidate remaining managers
- Standardize naming (Manager → Service)

### Potential Enhancements
- MIDI file import/export
- VST plugin support
- Multi-track recording
- Automation lanes
- MIDI learn functionality
- External MIDI clock sync
