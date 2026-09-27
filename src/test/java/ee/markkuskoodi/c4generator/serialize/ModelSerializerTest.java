package ee.markkuskoodi.c4generator.serialize;

import ee.markkuskoodi.c4generator.extract.ModelBuilder;
import ee.markkuskoodi.c4generator.model.AnalyzedRepository;
import ee.markkuskoodi.c4generator.model.Container;
import ee.markkuskoodi.c4generator.model.ContainerKind;
import ee.markkuskoodi.c4generator.model.Evidence;
import ee.markkuskoodi.c4generator.model.Model;
import ee.markkuskoodi.c4generator.model.Relationship;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModelSerializerTest {

    private final ModelSerializer serializer = new ModelSerializer();

    @Test
    void serializationIsIndependentOfContributionOrder() {
        Model ab = modelWithContainersInOrder("a", "b");
        Model ba = modelWithContainersInOrder("b", "a");
        assertThat(serializer.toJson(ab)).isEqualTo(serializer.toJson(ba));
    }

    @Test
    void repeatedSerializationIsByteIdentical() {
        Model model = modelWithContainersInOrder("a", "b");
        assertThat(serializer.toJson(model)).isEqualTo(serializer.toJson(model));
    }

    @Test
    void jsonUsesLfLineEndingsAndEndsWithNewline() {
        String json = serializer.toJson(modelWithContainersInOrder("a", "b"));
        assertThat(json).doesNotContain("\r").endsWith("\n");
    }

    @Test
    void roundTripPreservesTheModel(@TempDir Path tempDir) throws IOException {
        Model model = modelWithContainersInOrder("a", "b");
        Path file = tempDir.resolve("model.json");
        serializer.write(model, file);
        assertThat(serializer.read(file)).isEqualTo(model);
    }

    private Model modelWithContainersInOrder(String first, String second) {
        ModelBuilder builder = new ModelBuilder("Fixture System");
        builder.addRepository(new AnalyzedRepository("../repo", "0123456789012345678901234567890123456789"));
        for (String name : List.of(first, second)) {
            builder.addContainer(new Container("container:com.example:" + name, name,
                    ContainerKind.APPLICATION, "Java", List.of(new Evidence(name + "/pom.xml"))));
        }
        builder.addRelationship(new Relationship(
                "container:com.example:a--uses--container:com.example:b",
                "container:com.example:a", "container:com.example:b",
                "Uses", "HTTP", List.of(new Evidence("a/pom.xml"))));
        return builder.build();
    }
}