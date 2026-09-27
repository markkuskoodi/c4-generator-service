## MODIFIED Requirements

### Requirement: Structurizr DSL export
The tool SHALL export the generated model as a Structurizr DSL workspace file (`workspace.dsl`) in the output directory, containing the software system, its containers with technologies, the external software systems it communicates with, their relationships, and a container view of the system (FR-OUT-1). The exported DSL SHALL be accepted by Structurizr tooling without errors.

#### Scenario: Container diagram from a Spring Boot application
- **WHEN** the model of a single Spring Boot application with one database is exported
- **THEN** `workspace.dsl` defines the system, an application container, a data-store container, the relationship between them, and a container view — and Structurizr tooling loads it without errors

#### Scenario: Container diagram of a multi-container system
- **WHEN** the model of a system whose containers communicate with each other and with external systems is exported
- **THEN** `workspace.dsl` defines every container, every external system, and the relationships between them with their technologies, and Structurizr tooling loads it without errors

#### Scenario: Duplicate container names are disambiguated
- **WHEN** the exported model contains containers with equal names (e.g., two databases named alike on different products — Structurizr requires unique container names within a system)
- **THEN** the exporter deterministically disambiguates the display names (e.g., with the technology) and Structurizr tooling loads the workspace without errors

## ADDED Requirements

### Requirement: External systems rendered outside the system boundary

The exporter SHALL render external software systems outside the analyzed system's boundary, visually distinct from the system's own containers, so that a reader can tell at a glance which communication leaves the system (FR-RE-1, FR-OUT-1).

#### Scenario: External system placement
- **WHEN** a model containing an external software system is exported
- **THEN** the external system appears in the workspace as a software system outside the analyzed system's boundary, not as one of its containers

### Requirement: Unresolved relationships visibly marked

The exporter SHALL render every unresolved relationship in the container view, visually distinguished from resolved relationships and labeled with the unresolved target expression, so that what the analysis could not establish is visible in the diagram rather than absent from it (FR-RE-2, O5).

#### Scenario: Unresolved relationship rendered
- **WHEN** a model containing an unresolved relationship is exported
- **THEN** the container view shows an edge from the source container to a target that is marked as unresolved and labeled with the unresolved target expression, styled differently from resolved edges

#### Scenario: Unresolved targets are an export concern only
- **WHEN** the exporter renders unresolved relationships
- **THEN** the elements it introduces to do so exist only in the exported workspace and are derived deterministically from the unresolved target expression

### Requirement: Parallel relationships merged in the view

Where the model contains several relationships between the same pair of elements — for example one per messaging channel — the exporter SHALL render them as a single edge whose label names the technology and every channel, in a deterministic order, so that the diagram remains readable.

#### Scenario: Several channels between two containers
- **WHEN** a model containing several messaging relationships between the same two containers is exported
- **THEN** the container view shows one edge between them whose label names the messaging technology and all of the channels

#### Scenario: Merged label ordering is deterministic
- **WHEN** the same model is exported twice
- **THEN** the merged edge labels are identical in both exports

### Requirement: Infrastructure relationships excluded from the container view

The exporter SHALL omit relationships marked as infrastructure from the container view, so that the diagram shows the system's own communication rather than every container's dependency on the platform. The exported workspace SHALL still define the infrastructure containers themselves.

#### Scenario: Registry and configuration-server edges omitted
- **WHEN** a model whose containers all depend on a service registry and a configuration server is exported
- **THEN** the container view shows no edges to the registry or configuration server, while both remain defined as containers of the system
