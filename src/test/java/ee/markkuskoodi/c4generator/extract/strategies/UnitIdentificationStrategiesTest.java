package ee.markkuskoodi.c4generator.extract.strategies;

import ee.markkuskoodi.c4generator.extract.Diagnostics;
import ee.markkuskoodi.c4generator.extract.ModelBuilder;
import ee.markkuskoodi.c4generator.extract.RepositoryContext;
import ee.markkuskoodi.c4generator.model.Container;
import ee.markkuskoodi.c4generator.model.ContainerKind;
import ee.markkuskoodi.c4generator.model.Model;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Covers the unit-identification spec scenarios against the fixture projects. */
class UnitIdentificationStrategiesTest {

    private static final Path FIXTURES = Path.of("src/test/resources/fixtures");

    private record Result(Model model, Diagnostics diagnostics) {
    }

    private Result analyze(String fixture) {
        Path root = FIXTURES.resolve(fixture);
        assertThat(Files.isDirectory(root)).as("fixture exists: " + root).isTrue();
        RepositoryContext context = new RepositoryContext(fixture, root);
        ModelBuilder builder = new ModelBuilder("Fixture System");
        Diagnostics diagnostics = new Diagnostics();
        for (var strategy : new ee.markkuskoodi.c4generator.extract.ExtractionStrategy[]{
                new GradleUnitStrategy(), new MavenUnitStrategy(), new SpringDataStoreStrategy()}) {
            strategy.extract(context, builder, diagnostics);
        }
        return new Result(builder.build(), diagnostics);
    }

    @Test
    void mavenSpringBootAppBecomesContainerWithDataStores() {
        Result result = analyze("maven-app");
        Model model = result.model();

        assertThat(model.containers())
                .filteredOn(c -> c.kind() == ContainerKind.APPLICATION)
                .singleElement()
                .satisfies(app -> {
                    assertThat(app.id()).isEqualTo("container:org.springframework.boot:fixture-app");
                    assertThat(app.name()).isEqualTo("fixture-app");
                    assertThat(app.technology()).isEqualTo("Java, Spring Boot");
                    assertThat(app.evidence()).extracting("path").containsExactly("pom.xml");
                });

        assertThat(model.containers())
                .filteredOn(c -> c.kind() == ContainerKind.DATA_STORE)
                .extracting(Container::id)
                .containsExactly("datastore:mysql:fixturedb", "datastore:postgresql:fixturedb");

        assertThat(model.relationships()).hasSize(2)
                .allSatisfy(rel -> {
                    assertThat(rel.sourceId()).isEqualTo("container:org.springframework.boot:fixture-app");
                    assertThat(rel.technology()).isEqualTo("JDBC");
                    assertThat(rel.evidence()).isNotEmpty();
                });

        // application-cloud.properties uses ${DB_URL} without a default -> warn and skip
        assertThat(result.diagnostics().warnings())
                .anyMatch(w -> w.contains("DB_URL") && w.contains("cannot resolve"));
    }

    @Test
    void mavenLibraryIsNotAContainer() {
        assertThat(analyze("maven-library").model().containers()).isEmpty();
    }

    @Test
    void appWithoutDataStoreHasNoDataStoreContainersOrRelationships() {
        Model model = analyze("maven-app-nodb").model();
        assertThat(model.containers()).singleElement()
                .satisfies(c -> assertThat(c.kind()).isEqualTo(ContainerKind.APPLICATION));
        assertThat(model.relationships()).isEmpty();
    }

    @Test
    void gradleGroovyAppBecomesContainerWithH2DataStore() {
        Model model = analyze("gradle-groovy-app").model();

        assertThat(model.containers())
                .filteredOn(c -> c.kind() == ContainerKind.APPLICATION)
                .singleElement()
                .satisfies(app -> {
                    assertThat(app.id()).isEqualTo("container:com.example:groovy-app");
                    assertThat(app.technology()).isEqualTo("Java, Spring Boot");
                    assertThat(app.evidence()).extracting("path").containsExactly("build.gradle");
                });

        assertThat(model.containers())
                .filteredOn(c -> c.kind() == ContainerKind.DATA_STORE)
                .extracting(Container::id)
                .containsExactly("datastore:h2:groovydb");
    }

    @Test
    void gradleKotlinDslAppBecomesContainer() {
        Model model = analyze("gradle-kts-app").model();
        assertThat(model.containers()).singleElement()
                .satisfies(app -> {
                    assertThat(app.id()).isEqualTo("container:com.example:kts-app");
                    assertThat(app.name()).isEqualTo("kts-app");
                    assertThat(app.technology()).isEqualTo("Java, Spring Boot");
                });
    }

    @Test
    void uninterpretableGradleScriptWarnsAndContinues() {
        Result result = analyze("gradle-weird");
        assertThat(result.model().containers()).isEmpty();
        assertThat(result.diagnostics().warnings())
                .anyMatch(w -> w.contains("gradle-unit") && w.contains("no plugins block"));
    }
}