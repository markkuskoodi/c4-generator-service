# c4-generator-service

Generates C4 architecture models from a software system's source code, configuration,
and deployment definitions. The canonical output is a deterministic `model.json`;
diagrams are exported as a Structurizr DSL workspace (`workspace.dsl`) viewable with
[Structurizr Lite](https://docs.structurizr.com/lite).

## Build

```bash
./gradlew build installDist
```

The runnable distribution lands in `build/install/c4-generator-service/bin/c4-generator-service`.

## Usage

Create a project definition YAML:

```yaml
name: PetClinic
repositories:
  - path: ../spring-petclinic   # relative to this file
```

Generate the model and diagram workspace:

```bash
c4-generator-service generate project.yaml -o out/
# writes out/model.json and out/workspace.dsl
```

View the diagrams (the old `structurizr/lite` image is deprecated; use the consolidated one):

```bash
docker run -it --rm -p 8080:8080 -v $(pwd)/out:/usr/local/structurizr structurizr/structurizr local
# open http://localhost:8080/workspace/1/diagrams
```

The analyzed repository is never modified; all outputs go to the `-o` directory.

## M1 acceptance (milestone "done when")

Verified against [Spring PetClinic](https://github.com/spring-projects/spring-petclinic)
at commit `88e37c15cf6fc8490b01bc3e8e2c800cec1ac272`:

- Model: 3 containers — the `spring-petclinic` application (identified independently by
  both the Maven and the Gradle strategy from PetClinic's dual build, merged on the same
  stable id with united evidence `[build.gradle, pom.xml]`), plus MySQL and PostgreSQL
  data stores named `petclinic`, each with a JDBC relationship from the application.
  The default H2 profile declares no `spring.datasource.url`, so no H2 store is derived.
- The exported `workspace.dsl` loads in Structurizr (server mode `local`) and renders the
  container view; equal data-store display names are disambiguated with the product
  (`petclinic (MySQL)`, `petclinic (PostgreSQL)`) because Structurizr requires unique
  container names.
- Determinism: repeated runs produce byte-identical `model.json` and `workspace.dsl`.
- Self-analysis smoke test: running the tool on this repository yields
  `container:ee.markkuskoodi.c4generator:c4-generator-service` via the Gradle heuristics.