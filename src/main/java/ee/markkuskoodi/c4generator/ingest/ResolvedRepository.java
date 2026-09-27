package ee.markkuskoodi.c4generator.ingest;

import java.nio.file.Path;

/**
 * @param ref       the path reference exactly as written in the project definition
 * @param rootDir   the resolved repository working-tree root
 * @param commitSha full SHA of the checked-out HEAD commit (FR-IN-4)
 */
public record ResolvedRepository(String ref, Path rootDir, String commitSha) {
}