# Tasks — M2 Relationship Resolution

All code lives in `../../..`. Design decisions referenced as D11–D23 (`design.md`); M1 decisions D1–D10 are in the archived M1 change.

## 1. Model and serialization (specs: model-serialization)

- [ ] 1.1 Add dependencies: JavaParser (`javaparser-core`) and ASM; verify `./gradlew build` succeeds with both on the compile classpath
- [ ] 1.2 Implement the sealed `Target` interface with `Resolved` and `Unresolved(expression, reason)` and the `UnresolvedReason` enum (`VALUE_NOT_IN_ARTIFACTS`, `NO_MATCHING_COUNTERPART`) per D19; verify unit tests cover both variants and that exhaustive switching compiles without a default branch
- [ ] 1.3 Change `Relationship` to carry a `Target` plus optional `channel` and an infrastructure marker; verify existing relationship tests pass after migration
- [ ] 1.4 Add the `ExternalSystem` record and `Model.externalSystems`, sorted by id on build, and raise `schemaVersion` to 2; verify a model containing external systems serializes them in their own collection
- [ ] 1.5 Implement the custom Jackson serializer/deserializer pair keeping the flat JSON shape (`targetId`, or `unresolvedTarget` + `unresolvedReason`) per D19; verify a JSON round-trip preserves both target variants and that no element is emitted for an unresolved target
- [ ] 1.6 Extend `Ids` with `external:<host>` (D17), channel-qualified messaging relationship ids, and unresolved relationship ids derived from the slugged expression (D18); verify unit tests cover stability across re-derivation and that no id contains a path or counter
- [ ] 1.7 Add optional `applicationName` to `Container` with a `ModelBuilder` mutator (merge keeps existing values, so a setter is required), plus `ModelBuilder.addExternalSystem`; verify builder unit tests cover merge behavior for all three collections
- [ ] 1.8 Regenerate the M1 golden model files for `schemaVersion` 2; verify the full test suite passes

## 2. Multi-repository ingestion (specs: project-ingestion)

- [ ] 2.1 Accept several repositories in the project definition and resolve each independently; verify tests cover a multi-repository definition and an invalid path among valid ones naming the offending path
- [ ] 2.2 Record one analyzed-repository entry with its own commit SHA per referenced repository; verify a two-repository fixture yields two entries with the respective HEAD SHAs
- [ ] 2.3 Verify by test that listing the same repositories in a different order produces a byte-identical `model.json`, and that every referenced repository's working tree is unchanged after a run

## 3. Application name capture (specs: unit-identification)

- [ ] 3.1 Implement the `spring-appname` identification strategy reading `spring.application.name` across profile variants, its id sorting after the unit strategies and before `spring-datastore` (D16); verify the strategy ordering test still holds
- [ ] 3.2 Record the default-profile name on conflict and warn naming the conflicting files; verify tests cover name declared, no name declared, and conflicting names across profiles

## 4. Pipeline restructure and detector SPI (design D11, D13, D14)

- [ ] 4.1 Define the `Observation` record (source container, kind, target expression, channel, technology, evidence) and an observation buffer that deduplicates by content and unites evidence; verify a unit test shows the same observation from two sources collapsing into one with both evidence references
- [ ] 4.2 Define the `CallSiteDetector` SPI with `ServiceLoader` discovery sorted by id, and a project view exposing the identified containers to detectors; verify a test asserts deterministic detector ordering
- [ ] 4.3 Restructure `GenerateCommand` into the three phases — identification per repository, detection per repository, resolution once for the project; verify the existing end-to-end test passes unchanged against the M1 fixture
- [ ] 4.4 Implement attribution of a source file to its container by longest-prefix match against recorded module directories, warning on files under no known unit; verify unit tests cover a nested multi-module layout and an unattributable file

## 5. Configuration index and placeholder resolution (specs: relationship-resolution; design D15)

- [ ] 5.1 Build a `ConfigIndex` over every unit's `application*.{properties,yml,yaml}` recording key, value, source file and profile; verify a fixture with several profiles indexes each entry with its originating file and profile
- [ ] 5.2 Discover the configuration server from its own evidence (config-server dependency, `@EnableConfigServer`, declared native search locations) and match the files it serves to containers by application name; verify a two-repository fixture resolves a value served from the other repository
- [ ] 5.3 Implement placeholder resolution with the D15 precedence (config server, profile-specific local, default local, inline default), recursion depth cap, and cycle detection resolving to unresolved; verify unit tests cover each precedence level, a nested placeholder, and a cycle
- [ ] 5.4 Implement the internal-container preference as the tie-break across candidates and record the winning file and profile as evidence; verify tests cover profiles disagreeing with one internal candidate, profiles disagreeing with none internal, and a value absent from all artifacts

## 6. Resolver: classification and synchronous relationships (specs: relationship-resolution; design D16–D19)

- [ ] 6.1 Build the project-wide name index from application names and build coordinates, detecting names claimed by more than one container; verify a unit test covers an ambiguous name
- [ ] 6.2 Implement `RelationshipResolver` for `SYNCHRONOUS_CALL` observations: reduce the resolved value to a host, match against the name index, and emit an internal relationship, an external system plus relationship, or an unresolved relationship; verify tests cover all three outcomes driven by hand-built observation buffers
- [ ] 6.3 Emit one external system per host with one relationship per calling container, and record port and path on the relationship rather than the element (D17); verify a test with two containers calling the same host yields one external system and two relationships
- [ ] 6.4 Record an ambiguous name match as unresolved with a warning naming the target and the matching containers; verify the warning text and the resulting unresolved relationship in a test
- [ ] 6.5 Verify by test that a single-container project with only a data store produces no external systems and no unresolved relationships (O3)

## 7. Source detector (specs: relationship-resolution; design D12)

- [ ] 7.1 Implement the `java-source` detector: parse `src/main/java` of each unit with JavaParser, warn and skip unparsable files, continuing the analysis (NFR-7); verify a fixture containing a syntactically invalid file still yields observations from its siblings
- [ ] 7.2 Detect `RestTemplate` and `WebClient` call sites syntactically from imports, declared types and method names, capturing literal and placeholder targets; verify fixture-based tests cover both clients with a literal URL and with a placeholder
- [ ] 7.3 Detect declarative Feign clients, capturing the logical service name and an explicit `url` where present; verify a fixture interface yields an observation naming the logical service
- [ ] 7.4 Emit a non-statically-constant target as an unresolved observation with reason `VALUE_NOT_IN_ARTIFACTS`; verify a fixture with a computed URL produces an unresolved relationship rather than being dropped
- [ ] 7.5 Verify by end-to-end test on a repository containing no compiled classes that call-site detection still produces relationships

## 8. Bytecode detector (specs: relationship-resolution, cli; design D13)

- [ ] 8.1 Implement the `java-bytecode` detector over `target/classes` and `build/classes/java/main` using ASM, gated on the CLI option and never on the mere presence of class files; verify a test shows identical output with and without class files present while the option is off
- [ ] 8.2 Detect the same call-site vocabulary from class files, including annotation values carrying inlined compile-time constants; verify a fixture whose listener annotation references a constant field yields the literal channel
- [ ] 8.3 Warn naming the container and continue when the option is on but a container has no compiled classes; verify the run still exits 0 with a source-derived model
- [ ] 8.4 Verify by test that a relationship found by both detectors appears once carrying the evidence of both

## 9. Messaging relationships (specs: relationship-resolution; design D20)

- [ ] 9.1 Detect Kafka and RabbitMQ publication and consumption in both detectors, capturing the channel; verify fixtures cover `KafkaTemplate` send, `@KafkaListener`, `RabbitTemplate` send and `@RabbitListener`
- [ ] 9.2 Implement channel pairing in the resolver, emitting one relationship per channel from publisher to subscriber with both call sites as evidence; verify a test with two channels between the same pair yields two relationships
- [ ] 9.3 Emit one-ended channels as unresolved relationships with reason `NO_MATCHING_COUNTERPART`, keeping the known container as the source and distinguishing publication from subscription; verify tests cover a publisher with no subscriber and a subscriber with no publisher
- [ ] 9.4 Verify by test that a model containing messaging relationships contains no element representing the broker

## 10. Infrastructure marking (specs: relationship-resolution; design D21)

- [ ] 10.1 Mark relationships produced by the documented platform configuration keys (`eureka.client.serviceUrl.defaultZone`, `spring.cloud.config.uri`, equivalents) as infrastructure; verify tests cover a registry edge and a configuration-server edge
- [ ] 10.2 Verify by test that an ordinary container-to-container HTTP call and a gateway route are not marked as infrastructure

## 11. Diagram export (specs: diagram-export; design D22)

- [ ] 11.1 Emit external systems as sibling software systems outside the analyzed system's boundary; verify the exported DSL parses with `structurizr-dsl` and places them outside the boundary
- [ ] 11.2 Synthesize a placeholder element per unresolved relationship in the workspace only, derived deterministically from the unresolved expression, styled distinctly and labeled with it; verify repeated exports are byte-identical and the canonical model still contains no such element
- [ ] 11.3 Merge relationships between the same pair of elements into one edge whose label names the technology and every channel in sorted order; verify a model with several channels between two containers exports one labeled edge
- [ ] 11.4 Omit infrastructure relationships from the exported DSL while still emitting the infrastructure containers; verify the container view contains no such edge and both containers remain defined
- [ ] 11.5 Verify by test that exporting a model with containers, external systems, unresolved and infrastructure relationships parses with `structurizr-dsl` and is byte-identical across two exports

## 12. CLI (specs: cli)

- [ ] 12.1 Add the option enabling compiled-class analysis, off by default and listed in `--help`; verify the help output test and a run without the option against a built repository
- [ ] 12.2 Report the number of relationships recorded and how many are unresolved on successful generation; verify the end-to-end test asserts both counts in the output
- [ ] 12.3 Verify by end-to-end test on a multi-repository fixture that the run exits 0, writes both output files, and leaves every repository's working tree untouched

## 13. Acceptance (milestone M2 "done when")

- [ ] 13.1 Run the CLI against the multi-repository petclinic-microservices reference system; verify the container diagram shows technology-labeled edges between services, external systems outside the boundary, and unresolved edges visibly marked
- [ ] 13.2 Run the CLI against the PiggyMetrics reference system; verify messaging relationships appear between services with their channels and that the broker is absent from the model
- [ ] 13.3 Run the CLI against the spring-petclinic monolith through the same pipeline; verify it yields a correct single-container model (O3)
- [ ] 13.4 Verify determinism on both microservice reference systems: two consecutive runs produce byte-identical `model.json` and `workspace.dsl`
- [ ] 13.5 Spot-check the recovered relationships of one reference system against a manually drawn reference, recording false positives and misses as input to M7's measurement
- [ ] 13.6 Record the pinned commit SHA of every reference system and the acceptance observations in the README
