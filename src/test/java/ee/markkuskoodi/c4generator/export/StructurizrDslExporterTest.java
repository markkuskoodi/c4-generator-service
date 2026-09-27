package ee.markkuskoodi.c4generator.export;

import com.structurizr.dsl.StructurizrDslParser;
import ee.markkuskoodi.c4generator.model.AnalyzedRepository;
import ee.markkuskoodi.c4generator.model.Container;
import ee.markkuskoodi.c4generator.model.ContainerKind;
import ee.markkuskoodi.c4generator.model.Evidence;
import ee.markkuskoodi.c4generator.model.Model;
import ee.markkuskoodi.c4generator.model.Relationship;
import ee.markkuskoodi.c4generator.model.SystemInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class StructurizrDslExporterTest {

    private final StructurizrDslExporter exporter = new StructurizrDslExporter();

    /** Hand-built model — no repository involved (diagram-export spec). */
    private Model handBuiltModel() {
        Container app = new Container("container:com.example:shop", "shop \"quoted\"",
                ContainerKind.APPLICATION, "Java, Spring Boot", List.of(new Evidence("pom.xml")));
        Container db = new Container("datastore:postgresql:shopdb", "shopdb",
                ContainerKind.DATA_STORE, "PostgreSQL", List.of(new Evidence("application.yml")));
        Relationship uses = new Relationship(
                "container:com.example:shop--uses--datastore:postgresql:shopdb",
                app.id(), db.id(), "Reads from and writes to", "JDBC",
                List.of(new Evidence("application.yml")));
        return new Model(Model.CURRENT_SCHEMA_VERSION,
                new SystemInfo("system:shop", "Shop"),
                List.of(new AnalyzedRepository("../shop", "0123456789012345678901234567890123456789")),
                List.of(app, db), List.of(uses));
    }

    @Test
    void exportedDslIsAcceptedByStructurizrTooling() {
        String dsl = exporter.export(handBuiltModel());
        assertThatCode(() -> new StructurizrDslParser().parse(dsl)).doesNotThrowAnyException();
    }

    @Test
    void exportedDslContainsContainerViewAndDataStoreStyling() throws Exception {
        String dsl = exporter.export(handBuiltModel());
        StructurizrDslParser parser = new StructurizrDslParser();
        parser.parse(dsl);
        var workspace = parser.getWorkspace();
        assertThat(workspace.getModel().getSoftwareSystems()).hasSize(1);
        var system = workspace.getModel().getSoftwareSystems().iterator().next();
        assertThat(system.getContainers()).hasSize(2);
        assertThat(workspace.getViews().getContainerViews()).hasSize(1);
        assertThat(system.getContainers())
                .filteredOn(c -> c.getTechnology().equals("PostgreSQL"))
                .singleElement()
                .satisfies(c -> assertThat(c.getTags()).contains("Data Store"));
    }

    @Test
    void duplicateContainerNamesAreDisambiguated() throws Exception {
        // Structurizr requires unique container names; two databases may share one
        // (found by the PetClinic acceptance run: MySQL + PostgreSQL both 'petclinic').
        Container app = new Container("container:com.example:app", "app",
                ContainerKind.APPLICATION, "Java", List.of(new Evidence("pom.xml")));
        Container mysql = new Container("datastore:mysql:petclinic", "petclinic",
                ContainerKind.DATA_STORE, "MySQL", List.of(new Evidence("a.properties")));
        Container postgres = new Container("datastore:postgresql:petclinic", "petclinic",
                ContainerKind.DATA_STORE, "PostgreSQL", List.of(new Evidence("b.properties")));
        Model model = new Model(Model.CURRENT_SCHEMA_VERSION,
                new SystemInfo("system:x", "X"),
                List.of(new AnalyzedRepository("../x", "0123456789012345678901234567890123456789")),
                List.of(app, mysql, postgres), List.of());

        String dsl = exporter.export(model);
        StructurizrDslParser parser = new StructurizrDslParser();
        parser.parse(dsl); // throws if names collide
        var system = parser.getWorkspace().getModel().getSoftwareSystems().iterator().next();
        assertThat(system.getContainers()).extracting("name")
                .containsExactlyInAnyOrder("app", "petclinic (MySQL)", "petclinic (PostgreSQL)");
    }

    @Test
    void repeatedExportIsByteIdentical() {
        Model model = handBuiltModel();
        assertThat(exporter.export(model)).isEqualTo(exporter.export(model));
    }
}