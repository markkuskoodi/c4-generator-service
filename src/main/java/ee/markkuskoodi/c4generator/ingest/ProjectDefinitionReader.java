package ee.markkuskoodi.c4generator.ingest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ProjectDefinitionReader {

    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

    public ProjectDefinition read(Path definitionFile) {
        if (!Files.isRegularFile(definitionFile)) {
            throw new IngestException("Project definition not found: " + definitionFile);
        }
        ProjectDefinition definition;
        try {
            definition = yaml.readValue(definitionFile.toFile(), ProjectDefinition.class);
        } catch (IOException e) {
            throw new IngestException(
                    "Cannot parse project definition " + definitionFile + ": " + e.getMessage(), e);
        }
        if (definition == null || definition.name() == null || definition.name().isBlank()) {
            throw new IngestException("Project definition " + definitionFile + " must declare a system 'name'");
        }
        if (definition.repositories() == null || definition.repositories().isEmpty()) {
            throw new IngestException("Project definition " + definitionFile + " must reference at least one repository");
        }
        for (ProjectDefinition.RepositoryRef ref : definition.repositories()) {
            if (ref == null || ref.path() == null || ref.path().isBlank()) {
                throw new IngestException("Project definition " + definitionFile + " contains a repository entry without a 'path'");
            }
        }
        return definition;
    }
}