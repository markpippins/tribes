# 🎶 MIDI Sequencer

A Java-based MIDI sequencer with step sequencing, real-time playback, and Redis-backed state management. Features both drum and melodic sequencers with a Swing-based UI.

## 🚀 Features

### Sequencing
- **Drum Sequencer** - 16-pad step sequencer with per-step velocity and timing
- **Melodic Sequencer** - Note-based sequencer with scale quantization
- **Pattern System** - Organize sequences into patterns and songs
- **Euclidean Patterns** - Generate rhythmic patterns algorithmically
- **Real-time Playback** - Low-latency MIDI clock for tight timing

### MIDI
- **Multi-device Support** - Route to internal synthesizer or external MIDI devices
- **Soundbank Management** - Load and manage custom soundbanks
- **Preset Management** - Bank and program change support
- **Channel Management** - Automatic MIDI channel allocation
- **MIDI Repair** - Automatic reconnection on device issues

### UI
- **Modern Look** - FlatLaf-based UI with theme support
- **Visual Feedback** - Real-time visualization of playback
- **Mixer Panel** - Per-channel volume and mute controls
- **Piano Roll** - Visual note input
- **Diagnostics** - Built-in system diagnostics

### State Management
- **Redis-backed** - Fast state persistence and retrieval
- **Session Management** - Save and load complete sessions
- **User Preferences** - Persistent user configuration
- **Frame State** - Remember window positions and sizes

## 🧰 Requirements

- **Java 17 or higher** - Required for compilation and runtime
- **Redis 6.0+** - Running on localhost:6379 (default)
- **Maven 3.6+** - For building the project
- **MIDI Device** - Internal synthesizer or external MIDI interface

### Platform Support
- Windows (tested)
- macOS (should work)
- Linux (should work)

## 🔧 Setup

### 1. Install Redis

**Windows:**
```powershell
# Using Chocolatey
choco install redis-64

# Or download from: https://github.com/microsoftarchive/redis/releases
```

**macOS:**
```bash
brew install redis
brew services start redis
```

**Linux:**
```bash
sudo apt-get install redis-server
sudo systemctl start redis
```

Verify Redis is running:
```bash
redis-cli ping
# Should return: PONG
```

### 2. Verify Java Installation

```bash
java -version
# Should show Java 17 or higher
```

If Java is not installed, download from [Adoptium](https://adoptium.net/).

### 3. Clone and Build

```bash
# Clone the repository
git clone <repository-url>
cd <repository-name>

# Build all modules
cd java
mvn clean package -DskipTests

# Or use the build script
./build.sh
```

### 4. Run the Application

```bash
# From the java directory
cd swing/beatsui/target
java -jar beatsui-*-jar-with-dependencies.jar

# Or double-click the JAR file in your file explorer
```

## 📖 Documentation

- **[ARCHITECTURE.md](ARCHITECTURE.md)** - Detailed architecture overview
- **[REFACTORING_SUMMARY.md](REFACTORING_SUMMARY.md)** - Recent refactoring changes
- **[java/README.md](java/README.md)** - Java module structure

## 🎹 Quick Start

1. **Launch the application** - The splash screen will show initialization progress
2. **Create a drum pattern** - Click on the grid to add steps
3. **Select sounds** - Choose drum sounds from the pad selector
4. **Press Play** - Use the transport controls to start playback
5. **Add melody** - Switch to melodic sequencer for note-based sequences
6. **Save your work** - Sessions are automatically saved to Redis

## ⚙️ Configuration

### Redis Configuration
Default connection: `localhost:6379`

To change Redis settings, modify `RedisConfig.java`:
```java
// java/core/src/main/java/com/angrysurfer/core/config/RedisConfig.java
```

### MIDI Configuration
- The application auto-discovers MIDI devices on startup
- Prefers Gervill (Java's internal synthesizer) by default
- External MIDI devices can be selected from the UI

### UI Configuration
- Theme can be changed from the menu: View → Theme
- Window positions and sizes are automatically saved
- User preferences persist across sessions

## 🏗️ Project Structure

```
java/
├── core/              # Core business logic
│   ├── api/          # Command bus and events
│   ├── model/        # Domain models
│   ├── redis/        # Redis persistence
│   ├── sequencer/    # Sequencer implementations
│   └── service/      # Business services
├── swing/beatsui/    # Swing UI application
│   ├── panel/        # UI panels
│   ├── widget/       # Custom widgets
│   └── visualization/ # Visualization engine
└── pom.xml           # Parent POM

db/                   # Redis Docker setup
web/                  # Web client (experimental)
```

## 🔍 Troubleshooting

### Redis Connection Failed
```
Error: Could not connect to Redis
```
**Solution:** Ensure Redis is running on port 6379
```bash
redis-cli ping
```

### No MIDI Output
```
Warning: No MIDI devices found
```
**Solution:** The internal synthesizer should work by default. Check Java Sound configuration.

### Application Won't Start
```
Error: Failed to initialize services
```
**Solution:** Check logs in `logs/beats.log` for detailed error messages

## 📡 Redis Data Structure

The application stores data in Redis with the following key patterns:

- `player:{id}` - Player configurations
- `instrument:{id}` - Instrument settings
- `session:{id}` - Session data
- `pattern:{id}` - Pattern definitions
- `drumseq:{id}` - Drum sequence data
- `meloseq:{id}` - Melodic sequence data
- `userconfig` - User preferences
- `framestate:{name}` - UI window states

## 🧪 Development

### Running Tests
```bash
cd java
mvn test
```

### Building Individual Modules
```bash
# Core module
cd java/core
mvn clean package

# UI module
cd java/swing/beatsui
mvn clean package
```

### Debugging
Enable debug logging by modifying `logback.xml`:
```xml
<logger name="com.angrysurfer" level="DEBUG"/>
```

## 🤝 Contributing

Contributions are welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📄 License

MIT License - See LICENSE file for details

## 🙏 Acknowledgments

- **FlatLaf** - Modern Look and Feel for Swing
- **Redis** - Fast in-memory data store
- **Java Sound API** - MIDI and audio support
- **SLF4J/Logback** - Logging framework
