# unit-identification

## Purpose

Identifying the containers of the analyzed system: deployable units from Maven and Gradle build files, the data stores each unit uses from Spring application configuration, and each container's technology (FR-UN-1, FR-UN-2, FR-UN-4).

## Requirements

### Requirement: Deployable unit identification from build files
The tool SHALL identify each Maven or Gradle module that produces an executable application as a container of the analyzed system, named from its build coordinates (FR-UN-1). Modules that produce only libraries SHALL NOT become containers. Build files SHALL be interpreted without being executed: Maven POMs are read as XML; Gradle build scripts are interpreted through static heuristics.

#### Scenario: Single-module Maven Spring Boot application
- **WHEN** the analyzed repository is a single-module Maven project building an executable Spring Boot application (e.g., Spring PetClinic)
- **THEN** the model contains exactly one application container for it, named from the Maven artifact coordinates

#### Scenario: Single-module Gradle Spring Boot application
- **WHEN** the analyzed repository is a single-module Gradle project (Groovy or Kotlin DSL) whose build script applies the Spring Boot plugin
- **THEN** the model contains exactly one application container for it, named from the Gradle project coordinates (settings file project name and declared group)

#### Scenario: Library module is not a container
- **WHEN** an analyzed Maven or Gradle module produces a library artifact with no executable entry point
- **THEN** no container is created for that module

#### Scenario: Uninterpretable Gradle build script
- **WHEN** a Gradle build script uses constructs the static heuristics cannot interpret
- **THEN** the tool reports a warning naming the file and continues, producing a model from what it could interpret rather than failing (NFR-7)

### Requirement: Container technology determination
The tool SHALL determine each identified container's technology from its build configuration and record it on the container (FR-UN-4).

#### Scenario: Spring Boot technology detected
- **WHEN** an identified unit's build file declares Spring Boot
- **THEN** the container's technology names Java and Spring Boot

### Requirement: Data store identification from application configuration
The tool SHALL identify the data stores a unit uses from its Spring application configuration files (`application.properties`/`application.yml`, including profile variants), represent each as a data-store container with its technology, and record a relationship from the unit to the data store labeled with the access technology (FR-UN-2).

#### Scenario: Relational database from datasource configuration
- **WHEN** a unit's application configuration declares a JDBC datasource
- **THEN** the model contains a data-store container whose technology names the database product from the JDBC URL, and a relationship from the unit to that data store

#### Scenario: No data store configured
- **WHEN** a unit's application configuration declares no data store
- **THEN** the model contains the unit container without data-store containers or relationships

### Requirement: Evidence-backed identification
Every container and relationship identified by this capability SHALL carry at least one evidence reference to the source artifact it was derived from (NFR-6), expressed as a repository-relative file path.

#### Scenario: Container evidence
- **WHEN** a container is identified from a build file
- **THEN** the container's evidence references include the repository-relative path of that build file