# beatgen-spring

`beatgen-spring` is the Spring Boot module that provides REST endpoints and the web/service layer for BeatGeneratorApp.

Key information

- ArtifactId: `beatgen-spring`
- Java target: module property shows Java 18 (parent uses Java 17). Align as necessary.
- Uses the parent BOM (`spring-boot-dependencies`) for dependency version management.

Notable dependencies

- Spring Boot starters: web, data-rest, data-jpa, websocket, webflux, actuator
- SpringDoc OpenAPI starter
- Jackson (managed by BOM)
- Spring Boot devtools (optional, provided by parent property)

Build and run

From the repo root:

```powershell
cd java/spring
mvn -DskipTests package
# Run the Spring Boot app
java -jar target/beatgen-spring-1.0-SNAPSHOT.jar
```

Testing

```powershell
cd java/spring
mvn test
```

Notes & refactor suggestions

- Normalise the `java.version` across parent and child modules.
- Add README sections documenting available REST endpoints and an example curl session.
- Add a small `application-local.yml` or example `application.properties` and document required environment variables (DB, Redis, etc.) for integration tests.
- Consider adding Spring Boot Actuator security or management endpoints documentation.
