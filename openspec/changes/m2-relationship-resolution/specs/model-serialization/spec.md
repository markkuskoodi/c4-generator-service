## MODIFIED Requirements

### Requirement: Canonical model file
The tool SHALL write the generated model to a single JSON file (`model.json`) in the output directory, formatted for human review and version control: pretty-printed, UTF-8, with deterministic key and element ordering (NFR-8). The file SHALL declare the version of the model format it was written in.

#### Scenario: Model file produced
- **WHEN** an analysis completes successfully
- **THEN** the output directory contains `model.json`, parseable as JSON, containing the system, its containers, the external systems it communicates with, their relationships, and the analyzed commit SHA of each analyzed repository

#### Scenario: Model format version declared
- **WHEN** an analysis completes successfully
- **THEN** `model.json` declares the model format version, and that version differs from the one written before external systems and unresolved relationships were representable

### Requirement: Deterministic output
Repeated executions of the tool on unchanged repositories SHALL produce a byte-identical `model.json` (NFR-2). The serialized model SHALL contain no timestamps, random values, or ordering derived from filesystem or hash-map iteration. Determinism SHALL hold irrespective of the order in which repositories are referenced by the project definition and irrespective of whether the analyzed repositories contain build output.

#### Scenario: Two runs, identical output
- **WHEN** the tool is run twice against the same repositories at the same commits
- **THEN** the two `model.json` files are byte-identical

#### Scenario: Repository order does not affect output
- **WHEN** the tool is run twice against the same repositories listed in a different order in the project definition
- **THEN** the two `model.json` files are byte-identical

### Requirement: Stable element identifiers
Every model element SHALL have an identifier derived from stable properties of the analyzed system (e.g., build coordinates for units, configuration keys for data stores, the traced target for external systems), such that re-analysis after changes that do not affect the architecture yields the same identifiers (NFR-3). Relationship identifiers SHALL be derived from the identifiers of the elements they connect together with the channel they take place on, where one applies, so that relationships remain identifiable across regenerations.

#### Scenario: Non-architectural change preserves identifiers
- **WHEN** the analyzed repositories change in ways that do not alter their build coordinates, data-store configuration, or communication targets (e.g., source edits, new commits)
- **THEN** re-analysis produces the same element and relationship identifiers as before

#### Scenario: External system identifier stability
- **WHEN** an external system is identified from a traced target and the analysis is repeated
- **THEN** the external system carries the same identifier as before

### Requirement: Evidence references in the model
The serialized model SHALL include, for every element and relationship, the evidence references collected during extraction (NFR-6), so a reader can trace each model element back to the source artifacts it was derived from. Where a relationship was derived from several artifacts, all of them SHALL be listed.

#### Scenario: Tracing an element to its source
- **WHEN** a reader inspects any element in `model.json`
- **THEN** the element lists at least one repository-relative source artifact path it was derived from

#### Scenario: Tracing a relationship to its sources
- **WHEN** a reader inspects a relationship in `model.json` that was derived from a call site and a configuration value
- **THEN** the relationship lists the source artifact paths of both

## ADDED Requirements

### Requirement: External systems in the serialized model

The serialized model SHALL represent external software systems in their own collection, distinct from the system's containers, so that consumers of the model can place them outside the system boundary without inferring their nature from their attributes.

#### Scenario: External system serialized separately
- **WHEN** the analysis identifies a communication target that is not a container of the analyzed system
- **THEN** `model.json` lists it among the external systems and not among the containers

#### Scenario: Relationship referencing an external system
- **WHEN** a container communicates with an external system
- **THEN** the relationship in `model.json` names the external system by its identifier

### Requirement: Unresolved relationships in the serialized model

The serialized model SHALL represent a relationship whose target could not be established without naming a target element, and SHALL instead record the expression identifying the unknown target and the reason the target could not be resolved (FR-RE-2). The reason SHALL distinguish a target value that is absent from the analyzed artifacts from a counterpart that lies outside the analyzed project. No element SHALL be introduced into the model to stand in for an unresolved target.

#### Scenario: Unresolved relationship serialized
- **WHEN** the analysis records a relationship whose target could not be resolved
- **THEN** `model.json` contains that relationship without a target element identifier, carrying the unresolved target expression and the reason it is unresolved

#### Scenario: No placeholder elements
- **WHEN** a model containing unresolved relationships is serialized
- **THEN** the model's containers and external systems contain no element representing an unresolved target

### Requirement: Communication attributes on relationships

The serialized model SHALL record, on each relationship, the communication technology where it was determinable, the channel the communication takes place on where the communication is asynchronous, and whether the relationship is platform infrastructure rather than communication between the system's own parts.

#### Scenario: Messaging channel recorded as its own attribute
- **WHEN** a relationship representing asynchronous communication is serialized
- **THEN** the channel is recorded as a distinct attribute of the relationship rather than embedded in its description

#### Scenario: Infrastructure relationship marked
- **WHEN** a relationship to a service registry or configuration server is serialized
- **THEN** the relationship carries a marker identifying it as infrastructure
