package ee.markkuskoodi.c4generator.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/** The software system under analysis, named by the project definition. */
@JsonPropertyOrder({"id", "name"})
public record SystemInfo(String id, String name) {
}