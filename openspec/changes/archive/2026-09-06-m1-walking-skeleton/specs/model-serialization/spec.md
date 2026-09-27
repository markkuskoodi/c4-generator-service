# model-serialization

## Purpose

The canonical serialized form of the generated architecture model — deterministic, diffable, evidence-carrying JSON with stable element identifiers — which is the tool's primary product and the input to all exporters (NFR-2, NFR-3, NFR-6, NFR-8).

## ADDED Requirements

### Requirement: Canonical model file
The tool SHALL write the generated model to a single JSON file (`model.json`) in the output directory, formatted for human review and version control: pretty-printed, UTF-8, with deterministic key and element ordering (NFR-8).

#### Scenario: Model file produced
- **WHEN** an analysis completes successfully
- **THEN** the output directory contains `model.json`, parseable as JSON, containing the system, its containers, their relationships, and the analyzed commit SHA

### Requirement: Deterministic output
Repeated executions of the tool on an unchanged repository SHALL produce a byte-identical `model.json` (NFR-2). The serialized model SHALL contain no timestamps, random values, or ordering derived from filesystem or hash-map iteration.

#### Scenario: Two runs, identical output
- **WHEN** the tool is run twice against the same repository at the same commit
- **THEN** the two `model.json` files are byte-identical

### Requirement: Stable element identifiers
Every model element SHALL have an identifier derived from stable properties of the analyzed system (e.g., build coordinates for units, configuration keys for data stores), such that re-analysis after changes that do not affect the architecture yields the same identifiers (NFR-3).

#### Scenario: Non-architectural change preserves identifiers
- **WHEN** the analyzed repository changes in ways that do not alter its build coordinates or data-store configuration (e.g., source edits, new commits)
- **THEN** re-analysis produces the same element identifiers as before

### Requirement: Evidence references in the model
The serialized model SHALL include, for every element and relationship, the evidence references collected during extraction (NFR-6), so a reader can trace each model element back to the source artifact it was derived from.

#### Scenario: Tracing an element to its source
- **WHEN** a reader inspects any element in `model.json`
- **THEN** the element lists at least one repository-relative source artifact path it was derived from