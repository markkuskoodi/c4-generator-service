## MODIFIED Requirements

### Requirement: Project definition acceptance
The tool SHALL accept a project definition file that names the software system and references one or more repositories to analyze by local filesystem path, treating them together as a single unit of analysis (FR-IN-1, FR-IN-2). A project definition referencing one repository is the single-unit case of the same mechanism (O3).

#### Scenario: Valid project definition
- **WHEN** the tool is invoked with a project definition naming the system and referencing an existing local Git repository
- **THEN** the analysis runs and the generated model carries the system name from the definition

#### Scenario: Project definition referencing several repositories
- **WHEN** the project definition references several existing local Git repositories
- **THEN** all of them are analyzed and the generated model describes one system containing the containers identified in every referenced repository

#### Scenario: One of several referenced paths is invalid
- **WHEN** the project definition references several repositories and one of the referenced paths does not exist or is not a Git repository
- **THEN** the tool reports an error identifying the offending path and produces no model

#### Scenario: Referenced path is not a Git repository
- **WHEN** the project definition references a path that exists but is not a Git repository
- **THEN** the tool reports an error identifying the offending path and produces no model

#### Scenario: Project definition file missing or malformed
- **WHEN** the given project definition path does not exist or the file cannot be parsed
- **THEN** the tool reports an error naming the file and the parse problem and produces no model

### Requirement: Commit identifier recording
The generated model SHALL record, for each analyzed repository, the resolved commit identifier (full SHA) of that repository's checked-out revision (FR-IN-4).

#### Scenario: Commit SHA recorded
- **WHEN** analysis of a repository completes
- **THEN** the model contains the repository's full 40-character commit SHA, equal to the repository's current HEAD

#### Scenario: Commit SHA recorded per repository
- **WHEN** analysis of a project referencing several repositories completes
- **THEN** the model records each referenced repository separately with its own commit SHA

### Requirement: Read-only analysis
The tool SHALL NOT create, modify, or delete any file inside any analyzed repository (FR-IN-5). All outputs are written to the tool's designated output directory.

#### Scenario: Analyzed repository unchanged
- **WHEN** analysis of a repository completes, successfully or not
- **THEN** the repository's working tree is byte-identical to its state before the analysis

#### Scenario: Every referenced repository unchanged
- **WHEN** analysis of a project referencing several repositories completes, successfully or not
- **THEN** the working tree of every referenced repository is byte-identical to its state before the analysis
