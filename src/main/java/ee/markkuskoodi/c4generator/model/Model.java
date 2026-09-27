package ee.markkuskoodi.c4generator.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * Root of the canonical architecture model (schema version 1).
 * All lists are sorted by element id at build time so serialization is deterministic.
 */
@JsonPropertyOrder({"schemaVersion", "system", "repositories", "containers", "relationships"})
public record Model(
        int schemaVersion,
        SystemInfo system,
        List<AnalyzedRepository> repositories,
        List<Container> containers,
        List<Relationship> relationships
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public Model {
        repositories = List.copyOf(repositories);
        containers = List.copyOf(containers);
        relationships = List.copyOf(relationships);
    }
}