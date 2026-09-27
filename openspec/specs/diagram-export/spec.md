# diagram-export

## Purpose

Rendering the canonical model into formats existing C4 tooling can display — in M1, a Structurizr DSL workspace with a container view — keeping rendering fully decoupled from extraction (FR-OUT-1).

## Requirements

### Requirement: Structurizr DSL export
The tool SHALL export the generated model as a Structurizr DSL workspace file (`workspace.dsl`) in the output directory, containing the software system, its containers with technologies, their relationships, and a container view of the system (FR-OUT-1). The exported DSL SHALL be accepted by Structurizr tooling without errors.

#### Scenario: Container diagram from a Spring Boot application
- **WHEN** the model of a single Spring Boot application with one database is exported
- **THEN** `workspace.dsl` defines the system, an application container, a data-store container, the relationship between them, and a container view — and Structurizr tooling loads it without errors

#### Scenario: Duplicate container names are disambiguated
- **WHEN** the exported model contains containers with equal names (e.g., two databases named alike on different products — Structurizr requires unique container names within a system)
- **THEN** the exporter deterministically disambiguates the display names (e.g., with the technology) and Structurizr tooling loads the workspace without errors

### Requirement: Export derives from the serialized model only
Exporters SHALL take the canonical model as their only input and SHALL NOT re-analyze the repository (separation required so exporters remain pluggable, NFR-5).

#### Scenario: Export without repository access
- **WHEN** an export is produced from an existing `model.json`
- **THEN** the resulting `workspace.dsl` reflects that model's content without reading the analyzed repository

### Requirement: Deterministic export
Exporting the same model twice SHALL produce byte-identical DSL output (NFR-2).

#### Scenario: Repeated export
- **WHEN** the same `model.json` is exported twice
- **THEN** the two `workspace.dsl` files are byte-identical