# project-ingestion

## Purpose

Accepting a project definition that describes the software system to analyze, resolving the referenced repositories, and guaranteeing the analysis leaves them untouched (FR-IN-1, FR-IN-2, FR-IN-4, FR-IN-5).

## Requirements

### Requirement: Project definition acceptance
The tool SHALL accept a project definition file that names the software system and references the repository to analyze by local filesystem path (FR-IN-1, FR-IN-2; single repository in M1).

#### Scenario: Valid project definition
- **WHEN** the tool is invoked with a project definition naming the system and referencing an existing local Git repository
- **THEN** the analysis runs and the generated model carries the system name from the definition

#### Scenario: Referenced path is not a Git repository
- **WHEN** the project definition references a path that exists but is not a Git repository
- **THEN** the tool reports an error identifying the offending path and produces no model

#### Scenario: Project definition file missing or malformed
- **WHEN** the given project definition path does not exist or the file cannot be parsed
- **THEN** the tool reports an error naming the file and the parse problem and produces no model

### Requirement: Commit identifier recording
The generated model SHALL record the resolved commit identifier (full SHA) of the analyzed repository's checked-out revision (FR-IN-4).

#### Scenario: Commit SHA recorded
- **WHEN** analysis of a repository completes
- **THEN** the model contains the repository's full 40-character commit SHA, equal to the repository's current HEAD

### Requirement: Read-only analysis
The tool SHALL NOT create, modify, or delete any file inside the analyzed repository (FR-IN-5). All outputs are written to the tool's designated output directory.

#### Scenario: Analyzed repository unchanged
- **WHEN** analysis of a repository completes, successfully or not
- **THEN** the repository's working tree is byte-identical to its state before the analysis