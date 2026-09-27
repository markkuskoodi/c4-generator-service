## Purpose

Recovering the relationships between the analyzed system's containers and the external systems they communicate with — from call sites in source code and from the configuration values those call sites reference — classifying each target as internal or external, and representing explicitly what the available artifacts cannot resolve (FR-RE-1, FR-RE-2, O5).

## ADDED Requirements

### Requirement: Synchronous call-site detection

The tool SHALL detect outgoing synchronous calls made through Spring's HTTP clients — `RestTemplate`, `WebClient`, and declarative Feign clients — in the Java sources of each identified container, and SHALL record a relationship from that container to the called target labeled with the communication technology (FR-RE-1). Detection SHALL NOT require the analyzed system to be compiled.

#### Scenario: Call through an HTTP client with a literal target
- **WHEN** a container's source contains a call through an HTTP client whose target address is a literal value
- **THEN** the model contains a relationship from that container to the target, labeled with the communication technology

#### Scenario: Declarative HTTP client naming a logical service
- **WHEN** a container declares a Feign client naming a logical service rather than an address
- **THEN** the model contains a relationship from that container to that logical service name as its target

#### Scenario: Analysis of an uncompiled checkout
- **WHEN** the analyzed repositories contain no compiled classes
- **THEN** call-site detection still produces relationships from the sources alone

#### Scenario: Unparsable source file
- **WHEN** a Java source file cannot be parsed
- **THEN** the tool reports a warning naming the file and continues, producing relationships from the files it could parse (NFR-7)

### Requirement: Optional analysis of compiled classes

The tool SHALL offer call-site detection from compiled classes as an additional, separately enabled analysis. It SHALL be performed only when the user explicitly requests it, never merely because compiled classes happen to be present, so that repeated runs on an unchanged codebase produce identical output regardless of whether the analyzed system has been built (NFR-2). When enabled, its findings SHALL be merged with those from source analysis, and a relationship found by both SHALL appear once, carrying the evidence of both.

#### Scenario: Compiled classes present but analysis not requested
- **WHEN** the analyzed repositories contain compiled classes and the user has not requested compiled-class analysis
- **THEN** the generated model is identical to the model generated from the same repositories without compiled classes present

#### Scenario: Compiled-class analysis requested and classes available
- **WHEN** the user requests compiled-class analysis and the analyzed containers have been built
- **THEN** relationships recovered from compiled classes appear in the model alongside those recovered from sources

#### Scenario: Compiled-class analysis requested but classes absent
- **WHEN** the user requests compiled-class analysis and a container has no compiled classes
- **THEN** the tool reports a warning naming the container and completes the analysis using source-derived relationships (NFR-7)

#### Scenario: Relationship found by both analyses
- **WHEN** the same relationship is recovered from both a source call site and a compiled class
- **THEN** the model contains one relationship carrying the evidence references of both

### Requirement: Configuration-value tracing

Where a call site's target is given as a configuration placeholder rather than a literal, the tool SHALL trace the placeholder to a value using the artifacts of the analyzed project (FR-RE-1). The tool SHALL consult the container's own Spring application configuration including all profile variants, inline defaults declared with the placeholder, and configuration files served by a configuration server belonging to the project, wherever in the project's repositories that configuration server resides. Tracing SHALL NOT use values supplied by deployment definitions.

#### Scenario: Value declared in the container's own configuration
- **WHEN** a call site's target is a placeholder whose value is declared in the container's own application configuration
- **THEN** the relationship's target is resolved from that value

#### Scenario: Value served by the project's configuration server
- **WHEN** a call site's target is a placeholder whose value is not declared in the container's own configuration but is declared in configuration served by a configuration server located in another repository of the same project
- **THEN** the relationship's target is resolved from that value

#### Scenario: Placeholder with an inline default
- **WHEN** a call site's target is a placeholder declaring an inline default and no configuration file overrides it
- **THEN** the relationship's target is resolved from the inline default

#### Scenario: Profiles declaring different values
- **WHEN** a placeholder has different values in different configuration profiles and one of those values identifies a container of the analyzed project
- **THEN** that value is used to resolve the relationship, and the relationship's evidence names the configuration file and profile the value came from

#### Scenario: Profiles declaring different values, none internal
- **WHEN** a placeholder has different values in different profiles and none of them identifies a container of the analyzed project
- **THEN** the tool resolves the relationship from the value of the default profile where one exists, and the relationship's evidence names the configuration file and profile the value came from

#### Scenario: Value supplied only at deployment time
- **WHEN** a call site's target is a placeholder that no configuration file of the project declares and that has no inline default
- **THEN** the relationship is recorded as unresolved

### Requirement: Internal and external target classification

The tool SHALL classify each detected relationship target as internal to the analyzed system or as an external software system (FR-RE-1). A target is internal when it identifies a container of the analyzed project, matched against that container's declared application name or its build coordinates. Every other resolved target SHALL be represented as an external software system, distinct from the system's containers. Where a target matches more than one container, the tool SHALL NOT guess: it reports a warning and records the relationship as unresolved.

#### Scenario: Target matching a container's application name
- **WHEN** a resolved target identifies a container by the application name declared in that container's configuration
- **THEN** the relationship's target is that container and no external system is created

#### Scenario: Target matching a container's build coordinates
- **WHEN** a resolved target identifies a container by its build coordinates and no application name matches
- **THEN** the relationship's target is that container

#### Scenario: Target matching no container
- **WHEN** a resolved target identifies no container of the analyzed project
- **THEN** the model contains an external software system for that target and a relationship from the calling container to it

#### Scenario: One external target called by several containers
- **WHEN** two containers call the same external target
- **THEN** the model contains one external software system and one relationship from each calling container to it

#### Scenario: Target matching several containers
- **WHEN** a resolved target matches more than one container of the analyzed project
- **THEN** the tool reports a warning naming the target and the matching containers, and records the relationship as unresolved

#### Scenario: Single-container system
- **WHEN** the analyzed project consists of one container that makes no outgoing calls beyond its data store
- **THEN** the model contains that container and its data store, with no external systems and no unresolved relationships (O3)

### Requirement: Asynchronous messaging relationships

The tool SHALL detect message publication and consumption performed through Kafka and RabbitMQ in the analyzed containers and SHALL represent the resulting relationships between the communicating containers, labeled with the messaging technology and carrying the channel — topic, exchange, or queue — the communication takes place on (FR-RE-1). The message broker itself SHALL NOT be represented as an element of the model. Each channel SHALL yield its own relationship.

#### Scenario: Publisher and consumer both in the project
- **WHEN** one container publishes to a channel and another container of the same project consumes from that channel
- **THEN** the model contains a relationship from the publishing container to the consuming container, labeled with the messaging technology and recording the channel

#### Scenario: Broker is not an element
- **WHEN** a model containing messaging relationships is generated
- **THEN** the model contains no element representing the message broker

#### Scenario: Two containers communicating over several channels
- **WHEN** one container publishes to two channels that the same other container consumes
- **THEN** the model contains one relationship per channel, each recording its own channel and evidence

#### Scenario: Publication with no consumer in the project
- **WHEN** a container publishes to a channel that no container of the analyzed project consumes
- **THEN** the model contains an unresolved relationship whose source is the publishing container, which records the channel as the unresolved target and describes the container's role as publication

#### Scenario: Consumption with no publisher in the project
- **WHEN** a container consumes from a channel that no container of the analyzed project publishes to
- **THEN** the model contains an unresolved relationship whose source is the consuming container, which records the channel as the unresolved target and describes the container's role as subscription

### Requirement: Explicit representation of unresolved relationships

A relationship whose target the tool cannot establish SHALL be recorded in the model as an explicitly unresolved relationship rather than omitted or guessed (FR-RE-2, O5). It SHALL carry the expression that identifies the unknown target and the reason it could not be resolved, distinguishing a value that is absent from the analyzed artifacts from a counterpart that lies outside the analyzed project.

#### Scenario: Target absent from the analyzed artifacts
- **WHEN** a detected call's target is supplied only at deployment time
- **THEN** the model contains an unresolved relationship recording the unresolved expression and a reason identifying the value as absent from the analyzed artifacts

#### Scenario: Counterpart outside the analyzed project
- **WHEN** a detected messaging channel has only one end inside the analyzed project
- **THEN** the model contains an unresolved relationship recording the channel and a reason identifying the counterpart as outside the analyzed project

#### Scenario: Unresolved relationships are not silently dropped
- **WHEN** an analysis detects calls whose targets cannot be resolved
- **THEN** every such call is present in the model as an unresolved relationship and the count of unresolved relationships is reported to the user

### Requirement: Infrastructure relationships marked

Relationships to platform infrastructure that the analyzed containers depend on — service registry, configuration server, and gateway — SHALL be extracted and recorded in the model, and SHALL be marked as infrastructure so that consumers of the model can present them separately from the system's own communication.

#### Scenario: Registry and configuration-server dependencies recorded
- **WHEN** a container's configuration declares a service registry or configuration server belonging to the project
- **THEN** the model contains a relationship from that container to the registry or configuration-server container, marked as infrastructure

#### Scenario: Business relationships are not marked as infrastructure
- **WHEN** a container calls another container of the project through an HTTP client
- **THEN** that relationship is not marked as infrastructure

### Requirement: Evidence-backed relationships

Every relationship this capability records SHALL carry evidence references to all source artifacts it was derived from (NFR-6), expressed as repository-relative file paths: the call site, the configuration file whose value resolved the target where one was consulted, and, for a messaging relationship between two containers, the call sites at both ends.

#### Scenario: Evidence of a configuration-resolved call
- **WHEN** a relationship's target was resolved from a configuration value
- **THEN** the relationship's evidence references both the call site and the configuration file that supplied the value

#### Scenario: Evidence of a messaging relationship
- **WHEN** a relationship between a publishing and a consuming container is recorded
- **THEN** the relationship's evidence references the publication site and the consumption site
