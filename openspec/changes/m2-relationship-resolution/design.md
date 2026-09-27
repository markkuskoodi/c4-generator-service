# Design — M2 Relationship Resolution

## Context

See `proposal.md` for motivation and `../../../milestones.md` (M2) for the milestone contract; the behavior contract is in this change's `specs/`. M1's decisions D1–D10 (archived change `2026-09-06-m1-walking-skeleton`) stand unless a decision below supersedes them.

Two properties of the M1 code shape this design:

- **The pipeline is per-repository.** `GenerateCommand` loops over repositories and calls `StrategyRunner.run(context, builder, diagnostics)` for each. M2 cannot work this way: classifying a target as internal requires every container of every repository, and the configuration value that resolves a call site frequently lives in a configuration server in a *different* repository.
- **Detection and resolution are fused.** `SpringDataStoreStrategy` reads configuration and writes containers and relationships in one pass. That is adequate when the target is inside the file being read; it is not adequate when resolving a target needs a project-wide index.

Constraints carried from M1: determinism (NFR-2), identifier stability (NFR-3), pluggable extraction (NFR-5), evidence traceability (NFR-6), warn-and-continue robustness (NFR-7). New constraint from this change: the analysis must not require the analyzed system to have been built unless the user explicitly asks for compiled-class analysis.

## Goals / Non-Goals

**Goals:**

- Separate *detection* (find a call site; needs one file) from *resolution* (decide what it points at; needs the whole project), so that supporting a new client library means writing a detector and never touching resolution logic.
- Make the model's representation of an unknown target correct by construction rather than by convention.
- Keep the analysis build-free by default, and make the optional compiled-class path a genuine second source of findings rather than a confirmation pass.

**Non-Goals:**

- Type-accurate source analysis. The source detector is syntactic (D12); precision is traded for the build-free property and recovered by the optional bytecode detector.
- Any use of deployment definitions, including as a source of names or values (see proposal Non-goals). Compose and Kubernetes files are not read at this milestone.
- Performance work. NFR-4's budget is M7's concern; this design only avoids obviously quadratic behavior.

## Decisions

**D11 — Three-phase pipeline: identify, detect, resolve.** `GenerateCommand` changes from one loop into three stages:

1. *Identification*, per repository — today's `ExtractionStrategy` implementations, unchanged in shape: units, data stores, and now application names.
2. *Detection*, per repository — a new SPI (D13) whose implementations emit **observations** (D14) into a buffer. Detectors never write to the model.
3. *Resolution*, once for the whole project — a core `RelationshipResolver` consumes the observation buffer together with a name index (D17) and a configuration index (D16), and writes relationships and external systems into `ModelBuilder`.

Resolution is deliberately **not** pluggable. Pluggability serves NFR-5 where the variation lives — frameworks and languages differ in how a call site looks, not in what it means to match a host against a container. One resolver also means one place where classification, tracing, and the unresolved rules are implemented and unit-tested. *Alternatives:* phase-tagged strategies each resolving their own findings — rejected, every strategy would reimplement tracing and the implementations would drift; resolving lazily inside `ModelBuilder.build()` — rejected, it turns a pure sort-and-emit step into the analysis's most complex stage.

Ordering within each phase stays alphabetical by id, and the observation buffer is sorted before resolution, so output is independent of both discovery order and the order repositories appear in the project definition.

**D12 — Source detection with JavaParser, no symbol solver.** Java sources under each unit's `src/main/java` are parsed into ASTs; call sites are recognized syntactically, from imports, declared variable and field types, annotation names, and method names. No `JavaSymbolSolver`, because resolving Spring's own types requires the dependency jars on a solver classpath, and obtaining those means asking Maven or Gradle to resolve dependencies — the build requirement this milestone is meant to avoid. The cost is accepted and measurable: syntactic matching yields false positives (a method named `send` on an unrelated type) and misses (targets assembled through builder chains or computed at runtime). Unparsable files warn and are skipped (NFR-7).

**D13 — Detector SPI, with the bytecode detector as a second full detector.** A new `CallSiteDetector { String id(); void detect(RepositoryContext, ProjectView, ObservationSink, Diagnostics); }`, `ServiceLoader`-discovered and sorted by id, parallel to `ExtractionStrategy`. M2 ships `java-source` (D12) and `java-bytecode`. The bytecode detector reads class files with ASM from `target/classes` and `build/classes/java/main`, recognizes the same call-site vocabulary, and emits the same observation type. It runs only when the user passes the CLI option, never on mere presence of class files, so that two runs on an unchanged working tree agree regardless of whether a build happened in between (NFR-2).

It is a full detector rather than a verification pass because that is what makes the two paths comparable in M7, and because it can see things the source path cannot — most importantly the Java compiler inlines compile-time constants, so `@KafkaListener(topics = Topics.ORDERS)` carries the literal topic in the class file while the AST sees only a field reference. It is also the groundwork M4 needs for component extraction.

**D14 — The observation is the contract between detection and resolution.** A detector emits `Observation(sourceContainerId, kind, targetExpression, channel, technology, evidence)` where `kind` is one of `SYNCHRONOUS_CALL`, `PUBLISH`, `SUBSCRIBE`; `targetExpression` is the raw text as written (`http://customers-service/owners/{id}`, `${vets.service.url}`, or a bare Feign name); `channel` is set for messaging only. The detector attributes a file to a container by longest-prefix match of the file path against the module directories recorded in `ModelBuilder.Unit`; a file under no known unit is ignored with a warning.

Observations are deduplicated by their full content, so the same call site found by both detectors yields one observation carrying both evidence references.

**D15 — Placeholder resolution and the configuration index.** A `ConfigIndex` is built once, after identification, from every unit's `application*.{properties,yml,yaml}` (reusing M1's `SpringConfigReader`) plus the files served by a discovered configuration server. Each entry records key, value, source file, and profile. Resolution of `${key:default}` walks the candidate values for `key` in this precedence, mirroring how Spring actually resolves them at runtime:

1. configuration served by the project's configuration server for that application,
2. profile-specific local configuration,
3. default local configuration,
4. the inline default written at the call site.

Where several candidates remain, a candidate whose host matches a container of the project wins (the rule fixed in planning); otherwise the highest-precedence candidate wins. The winning candidate's file and profile become evidence. Nested placeholders resolve recursively with a depth cap, and a cycle resolves to unresolved rather than looping.

The configuration server is **discovered**, not declared: a container is treated as one when its own evidence says so — a Spring Cloud Config Server dependency, `@EnableConfigServer`, or declared native search locations — and the files it serves are then matched to containers by application name. This keeps the project definition format untouched and stays true to O1. *Alternative:* a `configSources:` field in the project definition — rejected as asking the user for knowledge the artifacts already carry, though it remains the fallback if discovery proves unreliable on a reference system.

**D16 — Classification through a project-wide name index.** After identification, a name index maps each container to its declared `spring.application.name` and its build coordinates (artifact or Gradle project name). A resolved target is reduced to a host — for a URL, the authority's host; for a bare name, the name itself — and looked up. A hit is an internal relationship to that container; a miss is an external software system. A host matching more than one container produces a warning and an unresolved relationship, because guessing here would silently corrupt the topology.

Application names are captured by a new identification strategy `spring-appname`, whose id sorts after the unit strategies and before `spring-datastore`, preserving the ordering contract M1 established. `ModelBuilder` gains a mutator for it, since its merge policy keeps existing field values.

**D17 — External system identity is the host alone.** `external:<host>`; port and path stay on the relationship. Endpoint and port changes therefore do not churn identifiers (NFR-3), and a host offering several APIs appears as one external system — a deliberate simplification, recorded as a known limit.

**D18 — Identifiers for relationships and unresolved targets.** Synchronous: `<sourceId>--calls--<targetId>`. Messaging: `<sourceId>--publishes--<targetId>#<channel-slug>`, so one relationship per channel keeps a stable identity across regenerations (NFR-3, and M6's diff depends on it). Unresolved: `<sourceId>--calls--unresolved:<slug-of-expression>`, the expression stripped of placeholder syntax before slugging. Identifiers never contain file paths, counters, or hashes of unstable input, per D8.

**D19 — The target is a sealed type; the JSON stays flat.** `Relationship` carries a `Target`, a sealed interface with `Resolved(String elementId)` and `Unresolved(String expression, UnresolvedReason reason)`, where the reason is `VALUE_NOT_IN_ARTIFACTS` (no value could be established from the analyzed artifacts — an undefined placeholder, or an expression that is not statically constant) or `NO_MATCHING_COUNTERPART` (the value is known, but no container of the project sits at the other end). A relationship that is both resolved and unresolved is then unrepresentable, and every consumer is forced by the compiler to handle both cases.

The serialized shape stays flat, as the specs describe — `targetId`, or `unresolvedTarget` plus `unresolvedReason` — through a custom Jackson serializer/deserializer pair on `Target`. `Relationship` additionally gains `channel` and an infrastructure marker; `Model` gains an `externalSystems` list; `Container` gains an optional `applicationName`. `schemaVersion` becomes 2.

**D20 — Messaging pairs by channel name.** The resolver indexes observations by channel: every `PUBLISH`/`SUBSCRIBE` pair on the same channel becomes a relationship from publisher to subscriber, carrying the messaging technology and the channel, with both call sites as evidence. A channel with publishers but no subscribers yields one unresolved relationship per publisher, described as publication; subscribers with no publishers yield the mirror case, described as subscription — in both directions the known container stays the relationship's source, so `sourceId` is never null. The broker is never an element (planning decision); it may return as an infrastructure node in M5.

**D21 — Infrastructure marking is driven by the configuration key.** An observation derived from a known platform key — `eureka.client.serviceUrl.defaultZone`, `spring.cloud.config.uri`, and their documented equivalents — produces a relationship marked as infrastructure. The key set is small, explicit, and auditable. Gateway routes are *not* marked: a gateway forwarding to a service is genuine request flow and among the most informative edges in the diagram. *Alternative:* marking every edge whose target is an infrastructure container — rejected, it would also swallow a genuine business call to such a container.

**D22 — Exporter changes.** External systems are emitted as sibling `softwareSystem` elements outside the analyzed system's boundary. Unresolved relationships are rendered against a placeholder element that the *exporter* synthesizes from the unresolved expression, deterministically and only inside the workspace — the canonical model gains no such element (D19). Relationships between the same pair of elements are merged into a single edge whose label names the technology and every channel in sorted order. Infrastructure relationships are omitted from the DSL altogether rather than excluded through a view expression, which keeps the exporter independent of Structurizr's view-filter grammar; the infrastructure containers themselves are still emitted.

**D23 — Testing.** Unit tests on small synthetic fixtures as in M1, plus approval tests that assert whole `model.json` outputs — the form of test that actually catches determinism and identifier-stability regressions. The resolver is tested directly against hand-built observation buffers, independent of any parser. Reference-system runs stay manual at a pinned commit, with the result recorded; M7 turns that into measurement. M1's golden files are regenerated for `schemaVersion` 2.

## Risks / Trade-offs

- [Syntactic detection produces false positives and misses (D12)] → the bytecode detector covers the worst cases; spot-checks against the reference systems each milestone, per `../../../../milestones.md`; the gap is reported as a result in M7 rather than hidden.
- [RabbitMQ pairs publishers to *exchanges* and subscribers to *queues*, and the binding between them is declared separately] → name-based pairing (D20) will leave genuinely connected services as two one-ended unresolved edges. Truthful under O5, but it understates recall; reading binding declarations is the first refinement to consider if spot-checks show many such edges.
- [A channel with many publishers and many subscribers yields their cross product] → merged into one edge per pair at export (D22); flagged if a reference system makes it unreadable.
- [Host-only external identity merges unrelated APIs behind one host (D17)] → documented limit; splitting by path would trade it for identifier churn.
- [Remote-wins precedence (D15) misresolves a service that legitimately overrides a value locally] → evidence records the exact file and profile every value came from, so any edge can be checked in seconds.
- [Configuration-server discovery fails on a layout neither reference system exhibits] → warn and leave the affected values unresolved; the declared `configSources:` fallback stays available.
- [Parsing every Java source of a multi-service project is slow (NFR-4)] → parse per file, no whole-project symbol resolution (D12); measured in M7, not tuned here.
- [Schema change invalidates M1 fixtures] → regenerated as part of this change (D23); no model files exist outside the repository's fixtures.

## Migration Plan

`schemaVersion` moves 1 → 2 and the exporter is updated in the same change. No models are persisted outside this repository's test fixtures, so the change is additive in practice: no consumer needs migrating, and rollback is reverting the change. The tool remains a stateless CLI, so there is no deployment step.

The reference systems are prepared outside this change (proposal Non-goals); implementation assumes they are present locally, and the acceptance run records the commit each was pinned at.

## Open Questions

- The exact spelling of the CLI option that enables compiled-class analysis — naming only, settled during implementation.
- The Java language level JavaParser is configured with for analyzed sources, and whether it needs to be configurable per project.
- Whether `model.json` gets a published JSON Schema now or when FR-OUT-3 diffing lands in M6 — carried over from M1, still deferrable.
- Diagram styling for external, unresolved, and infrastructure elements — cosmetic, decided while looking at rendered output.
