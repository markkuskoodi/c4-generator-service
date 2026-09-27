package ee.markkuskoodi.c4generator.ingest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectDefinitionReaderTest {

    private final ProjectDefinitionReader reader = new ProjectDefinitionReader();

    @Test
    void readsValidDefinition(@TempDir Path dir) throws IOException {
        Path file = Files.writeString(dir.resolve("project.yaml"), """
                name: PetClinic
                repositories:
                  - path: ../spring-petclinic
                """);
        ProjectDefinition definition = reader.read(file);
        assertThat(definition.name()).isEqualTo("PetClinic");
        assertThat(definition.repositories()).hasSize(1);
        assertThat(definition.repositories().getFirst().path()).isEqualTo("../spring-petclinic");
    }

    @Test
    void missingFileIsReported(@TempDir Path dir) {
        assertThatThrownBy(() -> reader.read(dir.resolve("nope.yaml")))
                .isInstanceOf(IngestException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void malformedYamlIsReported(@TempDir Path dir) throws IOException {
        Path file = Files.writeString(dir.resolve("broken.yaml"), "name: [unclosed\n  - ::::\n");
        assertThatThrownBy(() -> reader.read(file))
                .isInstanceOf(IngestException.class)
                .hasMessageContaining("Cannot parse");
    }

    @Test
    void missingNameIsReported(@TempDir Path dir) throws IOException {
        Path file = Files.writeString(dir.resolve("project.yaml"), """
                repositories:
                  - path: ../x
                """);
        assertThatThrownBy(() -> reader.read(file))
                .isInstanceOf(IngestException.class)
                .hasMessageContaining("name");
    }

    @Test
    void missingRepositoriesAreReported(@TempDir Path dir) throws IOException {
        Path file = Files.writeString(dir.resolve("project.yaml"), "name: X\n");
        assertThatThrownBy(() -> reader.read(file))
                .isInstanceOf(IngestException.class)
                .hasMessageContaining("repository");
    }
}