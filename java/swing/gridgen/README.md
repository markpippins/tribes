# Grid Visualizer

A standalone Swing application that displays an animated grid visualization. This is a simplified version of the original BeatGeneratorApp that focuses solely on the visualizer functionality.

## Features

- 32x64 grid of LED-style buttons
- Multiple visualization modes that automatically rotate
- Interactive - click to stop/start visualizer
- Beautiful FlatLaf look and feel
- Pure Java - no external dependencies on core sequencer functionality

## Key Components

- **App**: Main entry point - creates window with GridPanel
- **GridPanel**: 32x64 grid of buttons that starts visualizer automatically  
- **Visualizer**: Manages visualization scanning and automatic switching
- **IVisualizationHandler**: Interface for all visualization effects
- **GridButton**: Individual LED-style button component

## Build and Run

From the module folder:

```bash
cd java/swing/gridgen
mvn -DskipTests package
# The assembly plugin produces a jar-with-dependencies under target
java -jar target/gridgen-visualizer-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## Architecture

The visualizer automatically scans for all IVisualizationHandler implementations at startup and cycles through them randomly. Visualizations include:

- Classic effects (Starfield, Plasma, Fireworks, etc.)
- Matrix-style animations
- Scientific visualizations (Cellular, Crystal, etc.)
- Arcade game simulations
- Mathematical patterns

The application starts the visualizer automatically after 30 seconds of inactivity, or immediately on launch.

## Dependencies

- Java 18+
- FlatLaf for modern look and feel
- Lombok for boilerplate reduction
- SLF4J for logging
- Reflections for component scanning

No dependencies on the core beatgen sequencer - this is a completely standalone visualization module.