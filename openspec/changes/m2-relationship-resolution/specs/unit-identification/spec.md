## ADDED Requirements

### Requirement: Application name recording

The tool SHALL record on each identified container the application name declared in its Spring application configuration, where one is declared, so that logical service names appearing at call sites can be matched to containers (supports FR-RE-1). Where profile variants declare different application names, the tool SHALL record the name declared in the default configuration and report a warning naming the conflicting files.

#### Scenario: Application name declared
- **WHEN** an identified container's application configuration declares an application name
- **THEN** the container records that name, and the configuration file it was read from is among the container's evidence references

#### Scenario: No application name declared
- **WHEN** an identified container's application configuration declares no application name
- **THEN** the container records no application name and remains identifiable by its build coordinates alone

#### Scenario: Conflicting application names across profiles
- **WHEN** profile variants of a container's configuration declare different application names
- **THEN** the container records the name from the default configuration and the tool reports a warning naming the conflicting files (NFR-7)
