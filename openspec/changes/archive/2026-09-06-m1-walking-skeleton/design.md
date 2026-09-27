# Design — M1 Walking Skeleton

## Context

Greenfield: `c4-generator-service` is an empty repository. See `proposal.md` for motivation and `../../../milestones.md` (M1) for the milestone contract. Constraints that shape this design: determinism (NFR-2), stable identifiers (NFR-3), pluggable strategies (NFR-5), evidence traceability (NFR-6) — all structural and cheapest to establish now. User decisions already made: Gradle build, plain Java + picocli, Spring PetClinic as the acceptance target.

## Goals / Non-Goals

**Goals:**
- Establish the pipeline shape all later milestones extend: `ingest → extract (strategies) → model → serialize → export`.
- Establish the identifier scheme, evidence model, and deterministic serialization discipline.

**Non-Goals:**
- Multi-module Maven inheritance resolution, Gradle-target parsing, any FR-RE/CO/HK/DE behavior (see proposal Non-goals).
- GraalVM native image, distribution/packaging polish — a runnable Gradle `installDist`/fat jar is enough for M1.

## Decisions

**D1 — Single Gradle module, packages as seams.** One module with packages `cli`, `ingest`, `model`, `extract` (SPI + strategies), `export`. *Alternative:* multi-module build now — rejected; the walking skeleton should not guess module boundaries before M2 reveals them. Java 21 LTS, Gradle Kotlin DSL.

**D2 — Own model classes, not structurizr-java.** The canonical model needs fields Structurizr's model has no place for (evidence references, commit SHAs, later unresolved edges), and the model is the thesis's own contribution. The Structurizr exporter emits DSL text directly from the model. *Alternative:* build on `structurizr-java` and export via `structurizr-export` — rejected as the primary path because the canonical format would then be Structurizr's, inverting the architecture; kept cheap to adopt later inside the exporter if DSL emission grows hairy.

**D3 — DSL validity checked with `structurizr-dsl` as a test-only dependency.** The diagram-export spec requires the DSL to be accepted by Structurizr tooling; parsing every exported workspace with the real parser in a unit test enforces that without requiring structurizr-cli on the machine.

**D4 — Strategy SPI with deterministic ordering.** `ExtractionStrategy` implementations receive a repository context and contribute to a model builder; discovered via `ServiceLoader`, executed sorted by strategy id. All model collections are sorted on build. This is what makes NFR-2 hold regardless of discovery order. M1 ships two strategies: `maven-unit` (FR-UN-1/4) and `spring-datastore` (FR-UN-2).

**D5 — Maven parsing via `maven-model` (XML reader only).** Reads `pom.xml` without executing Maven or resolving parents. "Executable application" = declares `spring-boot-maven-plugin` or `war` packaging. *Alternative:* invoking Maven for an effective-pom — rejected: slow, non-hermetic, violates the spirit of FR-IN-5. Parent-inherited declarations are a known M1 blind spot (PetClinic doesn't need them).

**D5b — Gradle parsing via static heuristics, never execution.** `build.gradle`/`build.gradle.kts` are programs, so there is no data-reader equivalent of `maven-model`. M1 interprets them with targeted heuristics: the `plugins { }` block for `org.springframework.boot`/`application`/`war` (executable detection, FR-UN-1) and Spring Boot technology (FR-UN-4); the `group` assignment plus `rootProject.name` from `settings.gradle(.kts)` for coordinates. Anything the heuristics cannot interpret produces a warning and a partial result, not a failure (NFR-7). *Alternative:* Gradle Tooling API — accurate but executes build scripts, conflicting with the static-analysis-first constraint and dragging the target's Gradle version into the tool's runtime; documented as the fallback if reference systems outgrow the heuristics.

**D6 — Spring config via SnakeYAML + `java.util.Properties`.** Reads `application.{yml,yaml,properties}` and profile variants (`application-*.…`). Data stores come from `spring.datasource.url` JDBC URLs: scheme → product (mysql → MySQL…), path → database name. Every distinct URL across profiles becomes a data store (deduped by product + database name), each carrying its config file as evidence. PetClinic will therefore show H2, MySQL, and PostgreSQL stores — truthful, since the artifacts declare all three; profile-to-environment mapping is FR-DE-4's job in M5.

**D7 — Commit SHA via JGit.** No dependency on a `git` binary; reads HEAD directly. *Alternative:* shell out to `git rev-parse` — rejected for portability and testability.

**D8 — Identifier scheme (NFR-3): typed, hierarchical, derived from build/config coordinates.** `system:<slug-of-project-name>`, `container:<group>:<name>` (Maven groupId:artifactId; Gradle group + settings-file project name), `datastore:<product>:<database-name>`, relationship ids as `<source-id>--uses--<target-id>`. No counters, hashes of unstable inputs, or path-derived parts (paths change on checkout location). DSL identifiers are deterministic sanitizations of model ids with a collision check.

**D9 — Serialization via Jackson with hard determinism settings.** `schemaVersion: 1` field; map entries ordered by key; lists explicitly sorted; 2-space indent, `\n` line endings, UTF-8, trailing newline. A round-trip test and a run-twice byte-comparison test guard NFR-2/NFR-8.

**D10 — Project definition is YAML.** `name:` plus `repositories: [{path: …}]` (list-of-one in M1, shaped for M2's multi-repo). Relative paths resolve against the definition file's directory, not the CWD.

## Risks / Trade-offs

- [Maven parent/multi-module inheritance not resolved] → acceptable for PetClinic; strategy logs what it skipped rather than failing (NFR-7 spirit); revisit when a reference system needs it.
- [Gradle heuristics miss exotic build scripts (convention plugins, buildSrc, version catalogs)] → warn-and-continue per D5b; Gradle Tooling API documented as the accuracy fallback if a reference system needs it.
- [PetClinic upstream changes break acceptance] → pin a specific PetClinic commit SHA in the acceptance test docs/CI.
- [Hand-emitted DSL drifts from Structurizr grammar] → D3's parser-in-tests catches it on every build.
- [Multiple profile datasources look odd on one diagram] → documented behavior (D6); properly resolved by environments in M5.

## Open Questions

- Whether `model.json` gets a published JSON Schema now or when FR-OUT-3 diffing lands (M6) — deferrable, doesn't change the format discipline.
- Diagram styling/theme in the DSL (shapes for data stores etc.) — cosmetic, decide during implementation.