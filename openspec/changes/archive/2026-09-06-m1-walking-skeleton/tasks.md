# Tasks — M1 Walking Skeleton

All code lives in `../../../..`.

## 1. Project scaffolding

- [x] 1.1 Initialize Gradle project (Kotlin DSL, Java 21 toolchain, application plugin, JUnit 5) with package root and `.gitignore`
- [x] 1.2 Add dependencies: picocli, Jackson (databind + yaml), SnakeYAML, maven-model, JGit; test-only: structurizr-dsl, assertj
- [x] 1.3 Set up CI-friendly build entry (`./gradlew build installDist`) and a README stub describing invocation

## 2. Model and serialization (specs: model-serialization)

- [x] 2.1 Implement model classes: system, container (kind: application | data-store), relationship, evidence reference, analyzed-repository record (path ref + commit SHA), `schemaVersion`
- [x] 2.2 Implement stable identifier scheme per design D8, with unit tests for stability and collision behavior
- [x] 2.3 Implement deterministic Jackson serialization per design D9 (sorted keys/lists, fixed formatting); unit test: serialize same model twice → byte-identical
- [x] 2.4 Unit test: JSON round-trip preserves the model

## 3. Ingestion (specs: project-ingestion)

- [x] 3.1 Implement project definition YAML parsing per design D10, with clear errors for missing/malformed files
- [x] 3.2 Implement repository resolution: verify path is a Git repository, read HEAD SHA via JGit
- [x] 3.3 Unit tests: valid definition, missing file, malformed YAML, non-repo path, SHA equals fixture repo HEAD

## 4. Extraction SPI and strategies (specs: unit-identification)

- [x] 4.1 Define `ExtractionStrategy` SPI, repository context, model builder; ServiceLoader discovery with id-sorted execution order (design D4)
- [x] 4.2 Implement `maven-unit` strategy: parse `pom.xml` (maven-model), detect executable applications (spring-boot-maven-plugin or war), create container with technology and build-file evidence (FR-UN-1, FR-UN-4)
- [x] 4.3 Implement `spring-datastore` strategy: read `application.*` incl. profile variants, parse `spring.datasource.url`, create data-store containers plus unit→store relationships with evidence (FR-UN-2)
- [x] 4.4 Implement `gradle-unit` strategy: static heuristics over `build.gradle`/`build.gradle.kts` + `settings.gradle(.kts)` per design D5b — plugin detection, coordinates, warn-and-continue on uninterpretable scripts (FR-UN-1, FR-UN-4, NFR-7)
- [x] 4.5 Create fixture projects in test resources (Maven executable app with datasource; Maven library; app without datasource; Gradle Groovy-DSL app; Gradle Kotlin-DSL app; uninterpretable Gradle script) and cover all unit-identification scenarios

## 5. Structurizr DSL export (specs: diagram-export)

- [x] 5.1 Implement exporter interface taking only the canonical model (design D2); implement Structurizr DSL emitter: system, containers, relationships, container view, data-store tagging
- [x] 5.2 Unit test: exported DSL parses with structurizr-dsl (design D3); repeated export → byte-identical
- [x] 5.3 Unit test: export from a hand-built model works without any repository present

## 6. CLI (specs: cli)

- [x] 6.1 Implement picocli `generate` command wiring the pipeline: definition → ingest → strategies → model.json + workspace.dsl into the output directory
- [x] 6.2 Implement `--help`/no-args usage output and non-zero exits with stderr messages on failure
- [x] 6.3 End-to-end test against the fixture project: exit 0, both files present, repository working tree untouched (FR-IN-5 scenario)

## 7. Acceptance (milestone M1 "done when")

- [x] 7.1 Run the CLI against Spring PetClinic at a pinned commit; verify container diagram in Structurizr Lite shows the application and its database(s); record the pinned SHA and screenshot/output notes in the README
- [x] 7.2 Determinism check on PetClinic: two runs → byte-identical model.json and workspace.dsl
- [x] 7.3 Gradle smoke test: run the CLI against `c4-generator-service` itself (a Gradle project) and verify it appears as a container — self-analysis exercises the gradle-unit strategy on a real repository
