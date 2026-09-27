package ee.markkuskoodi.c4generator.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * One analyzed repository: the path reference exactly as written in the project
 * definition (not the resolved absolute path, which would differ per checkout
 * location) and the commit SHA of the analyzed revision (FR-IN-4).
 */
@JsonPropertyOrder({"path", "commitSha"})
public record AnalyzedRepository(String path, String commitSha) {
}