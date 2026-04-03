# 🎶 MIDI Sequencer

A lightweight MIDI sequencer built for real-time performance and automation. Powered by Redis for messaging and state management.

## 🚀 Features

- Step-based MIDI sequencing
- Real-time playback
- Redis-backed for fast interprocess communication
- Customizable patterns and timing
- Built with Java and Maven

## 🧰 Requirements

- A recent version of Java (Java 8 or higher)
- Redis running on the default port (6379)
- Bash (for running build.sh)
- Maven (optional if using the build script)

## 🔧 Setup

1. Install Redis  
   Make sure Redis is installed and running on localhost:6379:

2. Verify Java installation  
Ensure Java is installed and up to date:

3. Clone the repository  

4. Build the project  
Use the provided script to build the project with Maven:

5. Run the sequencer  
After building, navigate to the target/ directory.  
You can either:

- Double-click the generated *-with-dependencies.jar file  
OR  
- Run it manually from the command line:
  ```
  java -jar target/your-sequencer-with-dependencies.jar
  ```

## ⚙️ Configuration

- Redis must be running locally on port 6379
- MIDI output defaults to the system MIDI device
- No additional configuration is required to get started

## 📡 Redis Integration

Redis is used to:

- Store current sequence state


## 📄 License

MIT — do what you want, just give credit.

## 🙌 Credits

Built with coffee and late nights by [Mark Pippins].
