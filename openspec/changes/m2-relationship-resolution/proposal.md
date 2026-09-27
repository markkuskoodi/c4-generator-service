# M2 Relationship Resolution

## Why

Milestone M2 of `../../../../milestones.md` — the research core of the thesis. After M1 the tool produces containers but almost no relationships: a container diagram of a microservice system shows a set of disconnected boxes. M2 recovers container-to-container and container-to-external relationships statically from call sites and configuration, classifies each target as internal or external (FR-RE-1), and represents what cannot be resolved as explicitly unresolved rather than omitting or guessing it (FR-RE-2, O5). It also completes FR-IN-1 by grouping several repositories into one system, which is what makes a microservice system analyzable at all, and demonstrates O3 by running a monolith through the same pipeline as a single-unit case.

## What Changes

- **Multi-repository project definitions** (FR-IN-1 in full): a project definition groups several local repositories into one unit of analysis, each resolved to its own commit identifier (FR-IN-4). The monolith remains the single-repository case of the same mechanism (O3).
- **Call-site detection** for HTTP clients (`RestTemplate`, `WebClient`, `@FeignClient`) and messaging (Kafka, RabbitMQ) (FR-RE-1). Two pluggable strategies (NFR-5):
  - a **source-level strategy** parsing Java sources into an AST, which requires no build and is the default;
  - an **optional bytecode strategy**, enabled by an explicit CLI flag, that refines the result when compiled classes are available. It is opt-in rather than auto-detected so that repeated runs on an unchanged codebase stay identical (NFR-2). Assumption 2 (buildability) therefore still does not apply to M2.
- **Configuration-value tracing** (FR-RE-1): placeholder expressions in call sites are resolved against the module's own `application*.yml|.properties` across all profiles, against a Spring Cloud Config repository belonging to the project — a genuine cross-repository lookup — and against inline `${VAR:default}` defaults. Where profiles disagree, a candidate that matches a container of the project is preferred, and the evidence records the file and profile the value came from (NFR-6).
- **Internal/external classification** (FR-RE-1): a traced target is internal when it matches a container's `spring.application.name` or its build coordinates (artifact or Gradle project name); otherwise it is an external software system. Ambiguous matches — two containers claiming one name — are reported as a diagnostic and left unresolved rather than guessed.
- **External software systems become first-class model elements**: a new top-level `externalSystems` collection, distinct from containers, because in C4 an external system is a software system outside the system boundary and not a container within it.
- **Unresolved relationships** (FR-RE-2): a relationship whose target cannot be established carries no target identifier and instead records the unresolved expression and the reason it is unresolved, distinguishing a value that is absent from the analyzed artifacts from a counterpart that lies outside the analyzed project. Unresolved relationships are rendered as explicitly marked edges; no placeholder element enters the canonical model.
- **Messaging relationships are modeled between the communicating containers**, not through the broker: a producer and a consumer of the same channel are connected directly, labeled with the messaging technology and carrying the channel as a structured field. The broker itself does not become a model element at this milestone; it remains recoverable as an infrastructure node in M5. Where only one end of a channel is found in the project, the known container remains the relationship's source and the edge is unresolved, distinguishing publication from subscription.
- **One relationship per channel** is recorded in the model so that evidence and differences stay per-channel (NFR-6); the exporter merges parallel edges between the same pair into a single labeled arrow.
- **Infrastructure relationships** — service registry, configuration server, gateway — are extracted and kept in the model but tagged so the container view can exclude them; the model stays complete while the diagram stays readable.
- **Container diagram output** (FR-OUT-1) is extended accordingly: external systems outside the system boundary, technology-labeled edges, unresolved edges visibly marked, infrastructure edges filtered.
- **Acceptance target:** the multi-repository microservice reference systems produce a container diagram with technology-labeled edges between services, external systems separated from internal containers, and unresolved edges visibly marked; the monolith reference system produces a correct single-container model through the same pipeline.

## Capabilities

### New Capabilities

- `relationship-resolution`: recovering relationships from call sites and configuration, tracing configuration values across profiles and across repositories, classifying each target as internal or external, and representing unresolved targets explicitly (FR-RE-1, FR-RE-2).

### Modified Capabilities

- `project-ingestion`: a project definition groups more than one repository, each resolved and pinned independently (FR-IN-1 in full, FR-IN-4).
- `unit-identification`: each container additionally records the application name declared in its Spring configuration, so that logical service names used at call sites can be matched to containers (supports FR-RE-1).
- `model-serialization`: the canonical model gains a top-level external-systems collection and extends relationships with an unresolved marker, the unresolved target expression and reason, the messaging channel, and an infrastructure marker — all deterministic and diffable (NFR-2, NFR-8).
- `diagram-export`: external systems are rendered outside the system boundary, unresolved relationships are visibly marked, parallel per-channel edges are merged into one labeled arrow, and infrastructure relationships are excluded from the container view (FR-OUT-1).
- `cli`: a flag enables the optional bytecode-based call-site strategy (NFR-9).

## Non-goals

- **Preparing the reference systems.** Obtaining the microservice reference systems and splitting them into per-service repositories is environment setup for the thesis, not work on `c4-generator-service`. This change assumes those repositories exist locally.
- **Deployment recovery (M5, FR-DE-\*).** Docker Compose and Kubernetes manifests are not parsed at this milestone — neither for deployment nodes nor as a source of names for internal/external classification, and deployment-time environment values are not used to resolve configuration placeholders. A relationship whose target is supplied only at deployment time stays unresolved, which is precisely what FR-RE-2 requires.
- **The broker as a model element.** Messaging brokers are represented as the technology of a relationship; they may be recovered as infrastructure nodes in M5.
- **Client-side applications and the context diagram (M3, FR-UN-3, FR-HK-1, FR-OUT-1 context view)**, components (M4, FR-CO-\*), regeneration and diff (M6, FR-OUT-2, FR-OUT-3), runtime-evidence enrichment (M7, FR-RE-3), remote repository retrieval (FR-IN-3).
- **Precision and recall measurement (M7, NFR-1).** M2 spot-checks accuracy against the reference systems; the measured figures belong to M7.
- Anything on the out-of-scope list in `../../../../objectives-and-requirements.md`, in particular the dynamic diagram and interactive editing of the generated model.

## Impact

- **Code** (`../../..`): new extraction strategies for call sites and messaging; a configuration-resolution component shared by them; a name index built from containers for classification; `Model`, `Container`, `Relationship` and `Ids` extended for external systems and the new relationship fields; `ModelBuilder` gains relationship-level merging, which it currently has only for containers; `StructurizrDslExporter` gains boundary placement, unresolved styling, edge merging and infrastructure filtering; `GenerateCommand` gains the bytecode flag.
- **New dependencies**: a Java source parser (JavaParser) and, for the optional strategy, a bytecode reader (ASM).
- **Output format**: `model.json` gains fields and a collection. Since no model files are under version control outside the thesis fixtures, this is additive rather than breaking; the schema version is raised.
- **Thesis documents**: none are modified by this change. The design decisions recorded here feed the architecture chapter.
