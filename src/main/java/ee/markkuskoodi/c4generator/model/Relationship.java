package ee.markkuskoodi.c4generator.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonPropertyOrder({"id", "sourceId", "targetId", "description", "technology", "evidence"})
public record Relationship(
        String id,
        String sourceId,
        String targetId,
        String description,
        String technology,
        List<Evidence> evidence
) {
    public Relationship {
        evidence = evidence.stream().sorted().distinct().toList();
    }
}