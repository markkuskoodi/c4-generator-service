package ee.markkuskoodi.c4generator.model;

/**
 * Reference to the source artifact a model element was derived from (NFR-6),
 * as a repository-relative path with '/' separators.
 */
public record Evidence(String path) implements Comparable<Evidence> {
    @Override
    public int compareTo(Evidence other) {
        return path.compareTo(other.path);
    }
}