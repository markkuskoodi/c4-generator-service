package ee.markkuskoodi.c4generator.serialize;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import ee.markkuskoodi.c4generator.model.Model;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Deterministic serialization of the canonical model (design D9, NFR-2, NFR-8):
 * 2-space indent, LF line endings, sorted map keys, UTF-8, trailing newline.
 * Element ordering is the model's own (sorted at build time).
 */
public final class ModelSerializer {

    private final ObjectMapper mapper;

    public ModelSerializer() {
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter()
                .withObjectIndenter(new DefaultIndenter("  ", "\n"))
                .withArrayIndenter(new DefaultIndenter("  ", "\n"));
        this.mapper = JsonMapper.builder()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .defaultPrettyPrinter(printer)
                .build();
    }

    public String toJson(Model model) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(model) + "\n";
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void write(Model model, Path target) throws IOException {
        Files.writeString(target, toJson(model), StandardCharsets.UTF_8);
    }

    public Model read(Path source) throws IOException {
        return mapper.readValue(Files.readString(source, StandardCharsets.UTF_8), Model.class);
    }
}