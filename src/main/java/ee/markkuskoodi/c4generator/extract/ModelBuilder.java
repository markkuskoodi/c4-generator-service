package ee.markkuskoodi.c4generator.extract;

import ee.markkuskoodi.c4generator.model.AnalyzedRepository;
import ee.markkuskoodi.c4generator.model.Container;
import ee.markkuskoodi.c4generator.model.Evidence;
import ee.markkuskoodi.c4generator.model.Ids;
import ee.markkuskoodi.c4generator.model.Model;
import ee.markkuskoodi.c4generator.model.Relationship;
import ee.markkuskoodi.c4generator.model.SystemInfo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Accumulates extraction results and builds the canonical model. Elements with the
 * same id are merged (evidence united), and all collections are sorted by id on
 * build, so the result is independent of strategy contribution order (NFR-2).
 */
public final class ModelBuilder {

    /** An identified deployable unit and the module directory it was found in. */
    public record Unit(Container container, Path moduleDir) {
    }

    private final String systemName;
    private final List<AnalyzedRepository> repositories = new ArrayList<>();
    private final Map<String, Container> containers = new LinkedHashMap<>();
    private final Map<String, Relationship> relationships = new LinkedHashMap<>();
    private final Map<String, Unit> units = new LinkedHashMap<>();

    public ModelBuilder(String systemName) {
        this.systemName = systemName;
    }

    public void addRepository(AnalyzedRepository repository) {
        repositories.add(repository);
    }

    public void addUnit(Container container, Path moduleDir) {
        addContainer(container);
        units.putIfAbsent(container.id(), new Unit(containers.get(container.id()), moduleDir));
    }

    public void addContainer(Container container) {
        containers.merge(container.id(), container, ModelBuilder::mergeContainers);
    }

    public void addRelationship(Relationship relationship) {
        relationships.merge(relationship.id(), relationship, ModelBuilder::mergeRelationships);
    }

    /** The deployable units identified so far, sorted by container id. */
    public List<Unit> units() {
        return units.values().stream()
                .sorted(Comparator.comparing(unit -> unit.container().id()))
                .toList();
    }

    public Model build() {
        return new Model(
                Model.CURRENT_SCHEMA_VERSION,
                new SystemInfo(Ids.system(systemName), systemName),
                repositories.stream().sorted(Comparator.comparing(AnalyzedRepository::path)).toList(),
                containers.values().stream().sorted(Comparator.comparing(Container::id)).toList(),
                relationships.values().stream().sorted(Comparator.comparing(Relationship::id)).toList()
        );
    }

    private static Container mergeContainers(Container existing, Container added) {
        return new Container(existing.id(), existing.name(), existing.kind(), existing.technology(),
                unite(existing.evidence(), added.evidence()));
    }

    private static Relationship mergeRelationships(Relationship existing, Relationship added) {
        return new Relationship(existing.id(), existing.sourceId(), existing.targetId(),
                existing.description(), existing.technology(),
                unite(existing.evidence(), added.evidence()));
    }

    private static List<Evidence> unite(List<Evidence> a, List<Evidence> b) {
        return Stream.concat(a.stream(), b.stream()).sorted().distinct().toList();
    }
}