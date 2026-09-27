package ee.markkuskoodi.c4generator.ingest;

import java.util.List;

/**
 * The project definition (FR-IN-1, design D10): names the software system and
 * references the repositories to analyze. Single repository in M1; shaped as a
 * list for M2's multi-repository systems.
 */
public record ProjectDefinition(String name, List<RepositoryRef> repositories) {

    public record RepositoryRef(String path) {
    }
}