## ADDED Requirements

### Requirement: Compiled-class analysis option

The `generate` command SHALL provide an option that enables analysis of the analyzed containers' compiled classes in addition to their sources. The option SHALL be off by default, so that the tool requires no build of the analyzed system unless the user asks for one (NFR-9, NFR-2).

#### Scenario: Option listed in usage help
- **WHEN** the CLI is invoked with `--help`
- **THEN** the usage information lists the option enabling compiled-class analysis

#### Scenario: Generation without the option
- **WHEN** `generate` is invoked without the option against repositories containing compiled classes
- **THEN** the command exits with code 0 and the analysis uses sources only

#### Scenario: Generation with the option and no compiled classes
- **WHEN** `generate` is invoked with the option against a container that has not been built
- **THEN** the command writes a warning naming the container to standard error, exits with code 0, and produces a model derived from sources

### Requirement: Reporting of unresolved relationships

On successful generation the command SHALL report to the user how many relationships were recorded and how many of them are unresolved, so that the extent of what the analysis could not establish is visible without opening the model (O5).

#### Scenario: Summary includes unresolved count
- **WHEN** `generate` completes successfully on a project whose analysis produced unresolved relationships
- **THEN** the command's output states the number of relationships recorded and the number of them that are unresolved
