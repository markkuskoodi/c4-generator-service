# M1 Walking Skeleton

## Why

Milestone M1 of `../../../../../milestones.md`: establish the full pipeline — CLI → ingestion → extraction → canonical model → rendered diagram — end to end for the simplest real input, a single local Spring Boot repository. Every later milestone deepens this pipeline; M1 exists so the architectural decisions that cannot be retrofitted (stable element identifiers, evidence traceability, pluggable strategy structure, model serialization) are made while the system is still small.

## What Changes

- A new Gradle-built Java CLI application (picocli) is created in the `c4-generator-service` repository (NFR-9).
- The CLI accepts a project definition file referencing one local repository (FR-IN-1, FR-IN-2) and analyzes it read-only (FR-IN-5).
- Maven and Gradle build-file extraction strategies identify the repository's deployable unit as a container with its technology (FR-UN-1, FR-UN-4). Gradle build scripts are interpreted by static heuristics — they are never executed.
- A Spring configuration extraction strategy identifies the data stores the unit uses from `application.yml`/`.properties` (FR-UN-2).
- The resolved Git commit SHA of the analyzed repository is recorded in the model (FR-IN-4).
- The model is serialized as deterministic, VCS-friendly `model.json` with stable element identifiers and per-element evidence references (NFR-2, NFR-3, NFR-6, NFR-8).
- A Structurizr DSL exporter renders the container diagram from the model (FR-OUT-1, first diagram); exporters and extraction strategies are pluggable (NFR-5).
- Acceptance target: running the CLI against Spring PetClinic produces a container diagram showing the application and its database, viewable in Structurizr Lite.

## Capabilities

### New Capabilities

- `project-ingestion`: accepting a project definition, resolving the referenced local repository, recording its commit identifier, and guaranteeing read-only analysis (FR-IN-1, FR-IN-2, FR-IN-4, FR-IN-5).
- `unit-identification`: identifying deployable units from Maven and Gradle build files and data stores from Spring application configuration, with container technology (FR-UN-1, FR-UN-2, FR-UN-4 — Gradle via static heuristics).
- `model-serialization`: the canonical model format — elements, stable identifiers, evidence references, commit SHAs — written as deterministic, diffable `model.json` (NFR-2, NFR-3, NFR-6, NFR-8).
- `diagram-export`: exporting the model to Structurizr DSL and producing the container diagram view (FR-OUT-1).
- `cli`: the command-line interface — invocation, arguments, output locations, exit codes (NFR-9).

### Modified Capabilities

None — this is the first change; no specs exist yet.

## Non-goals

- Multi-repository project definitions, remote repository retrieval (M2, FR-IN-3).
- Relationships between containers, external systems, unresolved edges (M2, FR-RE-*). The M1 container diagram shows only the application, its data store, and the implicit uses-relationship derived from data-store configuration.
- Client-side applications (M3, FR-UN-3), components (M4, FR-CO-*), deployment recovery (M5, FR-DE-*), regeneration/diff (M6, FR-OUT-2/3).
- Gradle Tooling API integration — M1 parses Gradle build scripts statically and heuristically; asking Gradle itself for the project model (which executes build scripts) is a documented fallback for later, not part of this change.
- The architectural information descriptor (M3, FR-HK-1) and anything on the out-of-scope list in `../../../../../objectives-and-requirements.md`.
- Publishing/hosting of diagrams (M7 stretch, FR-OUT-4) — M1 only writes DSL locally.

## Impact

- New code: everything under `../../../..` (currently an empty repo) — Gradle build, CLI entry point, strategy SPI, two extraction strategies, model classes + JSON serialization, Structurizr DSL exporter.
- New dependencies: picocli; a YAML parser (project definition, Spring config); Jackson (model.json); JGit or `git` invocation (commit SHA); Maven model reader for `pom.xml` parsing.
- No existing code or APIs are affected. The thesis planning documents are unchanged by this proposal.
