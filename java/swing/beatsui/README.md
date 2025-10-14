# beatgen-swing (beatsui)

The Swing UI module contains a desktop user interface for BeatGeneratorApp. It depends on `beatgen-core` for the domain and sequencing logic.

Key information

- ArtifactId: `beatgen-swing`
- Packaging: jar (assembled with dependencies via Maven Assembly)
- Main class declared in assembly manifest: `com.angrysurfer.beats.App`
- Uses FlatLaf for look-and-feel

Build and run

From the module folder:

```powershell
cd java/swing/beatsui
mvn -DskipTests package
# The assembly plugin produces a jar-with-dependencies under target
java -jar target/beatgen-swing-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Testing

```powershell
cd java/swing/beatsui
mvn test
```

Notes & refactor suggestions

- Consider modernizing the UI to a separate module or adding a small CLI wrapper for headless testing.
- Document the public UI entry points (menu actions) and how they map to `beatgen-core` services.
- Add end-to-end tests that cover launching the UI and invoking core functions if CI can run headless (Xvfb or headless mode) or isolate logic into testable presenters.
