# cli

## Purpose

The command-line interface through which the tool is operated — invocation, arguments, output locations, exit codes, and error reporting — requiring no manual model authoring from the user (NFR-9).

## ADDED Requirements

### Requirement: Generate command
The tool SHALL provide a `generate` command that takes the path to a project definition file and an output directory, runs the analysis, and writes `model.json` and `workspace.dsl` to the output directory (NFR-9).

#### Scenario: Successful generation
- **WHEN** `generate` is invoked with a valid project definition and a writable output directory
- **THEN** the command exits with code 0 and the output directory contains `model.json` and `workspace.dsl`

#### Scenario: Failure reporting
- **WHEN** `generate` fails (invalid definition, unreadable repository, unwritable output directory)
- **THEN** the command exits with a non-zero code and writes a human-readable error message to standard error

### Requirement: Usage help
The CLI SHALL print usage information describing its commands and options when invoked with `--help` or with no arguments.

#### Scenario: Help output
- **WHEN** the CLI is invoked with `--help`
- **THEN** usage information listing the `generate` command and its parameters is printed and the exit code is 0