package ee.markkuskoodi.c4generator.extract;

/**
 * A pluggable extraction strategy (NFR-5, design D4). Implementations are discovered
 * via {@link java.util.ServiceLoader} and executed in ascending id order, so adding a
 * strategy never requires touching the pipeline and execution order is deterministic.
 */
public interface ExtractionStrategy {

    /** Unique, stable id; also the execution sort key. */
    String id();

    void extract(RepositoryContext context, ModelBuilder builder, Diagnostics diagnostics);
}