package ee.markkuskoodi.c4generator.extract.strategies;

import ee.markkuskoodi.c4generator.extract.Diagnostics;
import ee.markkuskoodi.c4generator.extract.ExtractionStrategy;
import ee.markkuskoodi.c4generator.extract.ModelBuilder;
import ee.markkuskoodi.c4generator.extract.RepositoryContext;
import ee.markkuskoodi.c4generator.model.Container;
import ee.markkuskoodi.c4generator.model.ContainerKind;
import ee.markkuskoodi.c4generator.model.Evidence;
import ee.markkuskoodi.c4generator.model.Ids;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Identifies deployable units from Maven build files (FR-UN-1, FR-UN-4, design D5).
 * POMs are read as plain XML — Maven is never executed and parent POMs are not
 * resolved. A module is an executable application if it declares the Spring Boot
 * Maven plugin or {@code war} packaging; library modules do not become containers.
 */
public final class MavenUnitStrategy implements ExtractionStrategy {

    private static final Set<String> SKIPPED_DIRS = Set.of(".git", "target", "build", "node_modules", "out");

    @Override
    public String id() {
        return "maven-unit";
    }

    @Override
    public void extract(RepositoryContext context, ModelBuilder builder, Diagnostics diagnostics) {
        for (Path pom : findPoms(context.rootDir(), diagnostics)) {
            analyzePom(pom, context, builder, diagnostics);
        }
    }

    private List<Path> findPoms(Path root, Diagnostics diagnostics) {
        try (Stream<Path> files = Files.walk(root)) {
            return files
                    .filter(p -> p.getFileName().toString().equals("pom.xml"))
                    .filter(p -> {
                        String relative = root.relativize(p).toString().replace('\\', '/');
                        // test trees hold fixtures, not deployable units
                        if (relative.contains("src/test/")) {
                            return false;
                        }
                        for (Path segment : root.relativize(p)) {
                            if (SKIPPED_DIRS.contains(segment.toString())) {
                                return false;
                            }
                        }
                        return true;
                    })
                    .sorted()
                    .toList();
        } catch (IOException e) {
            diagnostics.warn("maven-unit: cannot scan repository for pom.xml files: " + e.getMessage());
            return List.of();
        }
    }

    private void analyzePom(Path pom, RepositoryContext context, ModelBuilder builder, Diagnostics diagnostics) {
        Model model;
        try (Reader reader = Files.newBufferedReader(pom)) {
            model = new MavenXpp3Reader().read(reader);
        } catch (Exception e) {
            diagnostics.warn("maven-unit: cannot parse " + context.relativize(pom) + ": " + e.getMessage());
            return;
        }

        boolean springBoot = hasSpringBootPlugin(model);
        boolean war = "war".equals(model.getPackaging());
        if (!springBoot && !war) {
            return; // library or aggregator module, not a deployable unit
        }

        String group = model.getGroupId() != null ? model.getGroupId()
                : model.getParent() != null ? model.getParent().getGroupId() : null;
        String artifactId = model.getArtifactId();
        if (group == null || artifactId == null) {
            diagnostics.warn("maven-unit: " + context.relativize(pom)
                    + " has no resolvable groupId/artifactId; skipping");
            return;
        }

        String technology = springBoot ? "Java, Spring Boot" : "Java";
        Container container = new Container(
                Ids.container(group, artifactId),
                artifactId,
                ContainerKind.APPLICATION,
                technology,
                List.of(new Evidence(context.relativize(pom))));
        builder.addUnit(container, pom.getParent());
    }

    private boolean hasSpringBootPlugin(Model model) {
        if (model.getBuild() == null) {
            return false;
        }
        for (Plugin plugin : model.getBuild().getPlugins()) {
            if ("spring-boot-maven-plugin".equals(plugin.getArtifactId())) {
                return true;
            }
        }
        return false;
    }
}