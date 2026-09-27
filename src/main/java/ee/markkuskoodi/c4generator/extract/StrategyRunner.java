package ee.markkuskoodi.c4generator.extract;

import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

public final class StrategyRunner {

    private final List<ExtractionStrategy> strategies;

    /** Discovers all strategies on the classpath, ordered by id (design D4). */
    public static StrategyRunner discover() {
        return new StrategyRunner(StreamSupport
                .stream(ServiceLoader.load(ExtractionStrategy.class).spliterator(), false)
                .toList());
    }

    public StrategyRunner(List<ExtractionStrategy> strategies) {
        this.strategies = strategies.stream()
                .sorted(Comparator.comparing(ExtractionStrategy::id))
                .toList();
    }

    public void run(RepositoryContext context, ModelBuilder builder, Diagnostics diagnostics) {
        for (ExtractionStrategy strategy : strategies) {
            strategy.extract(context, builder, diagnostics);
        }
    }
}