package ee.markkuskoodi.c4generator.extract;

import java.nio.file.Path;

/** The repository a strategy analyzes. Strategies only ever read from it (FR-IN-5). */
public record RepositoryContext(String ref, Path rootDir) {

    /** Repository-relative path with '/' separators, for evidence references. */
    public String relativize(Path file) {
        return rootDir.relativize(file.normalize()).toString().replace('\\', '/');
    }
}