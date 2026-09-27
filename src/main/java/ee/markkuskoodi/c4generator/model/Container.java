package ee.markkuskoodi.c4generator.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonPropertyOrder({"id", "name", "kind", "technology", "evidence"})
public record Container(
        String id,
        String name,
        ContainerKind kind,
        String technology,
        List<Evidence> evidence
) {
    public Container {
        evidence = evidence.stream().sorted().distinct().toList();
    }
}