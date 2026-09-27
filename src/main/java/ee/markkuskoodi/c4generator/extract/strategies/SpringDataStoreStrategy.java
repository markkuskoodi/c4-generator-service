package ee.markkuskoodi.c4generator.extract.strategies;

import ee.markkuskoodi.c4generator.extract.Diagnostics;
import ee.markkuskoodi.c4generator.extract.ExtractionStrategy;
import ee.markkuskoodi.c4generator.extract.ModelBuilder;
import ee.markkuskoodi.c4generator.extract.RepositoryContext;
import ee.markkuskoodi.c4generator.model.Container;
import ee.markkuskoodi.c4generator.model.ContainerKind;
import ee.markkuskoodi.c4generator.model.Evidence;
import ee.markkuskoodi.c4generator.model.Ids;
import ee.markkuskoodi.c4generator.model.Relationship;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Identifies the data stores each unit uses from its Spring application configuration
 * (FR-UN-2, design D6). Runs after the unit strategies — strategy ids sort it last
 * (design D4). Every distinct datasource URL across profile files becomes a data
 * store; profile-to-environment mapping is deferred to M5 (FR-DE-4).
 */
public final class SpringDataStoreStrategy implements ExtractionStrategy {

    private static final Pattern CONFIG_FILE = Pattern.compile(
            "application(-[A-Za-z0-9_]+)?\\.(properties|ya?ml)");
    private static final Pattern PLACEHOLDER = Pattern.compile(
            "\\$\\{([^}:]+)(?::([^}]*))?}");
    private static final String DATASOURCE_URL_KEY = "spring.datasource.url";

    @Override
    public String id() {
        return "spring-datastore";
    }

    @Override
    public void extract(RepositoryContext context, ModelBuilder builder, Diagnostics diagnostics) {
        for (ModelBuilder.Unit unit : builder.units()) {
            Path resources = unit.moduleDir().resolve("src/main/resources");
            for (Path configFile : configFiles(resources, diagnostics)) {
                analyzeConfigFile(unit, configFile, context, builder, diagnostics);
            }
        }
    }

    private List<Path> configFiles(Path resources, Diagnostics diagnostics) {
        if (!Files.isDirectory(resources)) {
            return List.of();
        }
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(resources)) {
            for (Path file : stream) {
                if (CONFIG_FILE.matcher(file.getFileName().toString()).matches()) {
                    files.add(file);
                }
            }
        } catch (IOException e) {
            diagnostics.warn("spring-datastore: cannot list " + resources + ": " + e.getMessage());
        }
        return files.stream().sorted().toList();
    }

    private void analyzeConfigFile(ModelBuilder.Unit unit, Path configFile,
                                   RepositoryContext context, ModelBuilder builder, Diagnostics diagnostics) {
        Map<String, String> config;
        try (Reader reader = Files.newBufferedReader(configFile)) {
            config = configFile.getFileName().toString().endsWith(".properties")
                    ? SpringConfigReader.readProperties(reader)
                    : SpringConfigReader.readYaml(reader);
        } catch (Exception e) {
            diagnostics.warn("spring-datastore: cannot parse " + context.relativize(configFile)
                    + ": " + e.getMessage() + "; continuing (NFR-7)");
            return;
        }

        String url = config.get(DATASOURCE_URL_KEY);
        if (url == null) {
            return;
        }
        Optional<String> resolved = resolvePlaceholders(url, context.relativize(configFile), diagnostics);
        if (resolved.isEmpty()) {
            return;
        }
        Optional<JdbcUrls.DataStoreRef> ref = JdbcUrls.parse(resolved.get());
        if (ref.isEmpty()) {
            diagnostics.warn("spring-datastore: unrecognized datasource URL '" + url
                    + "' in " + context.relativize(configFile));
            return;
        }

        Evidence evidence = new Evidence(context.relativize(configFile));
        String storeId = Ids.dataStore(ref.get().product(), ref.get().database());
        builder.addContainer(new Container(
                storeId,
                ref.get().database(),
                ContainerKind.DATA_STORE,
                ref.get().product(),
                List.of(evidence)));
        String unitId = unit.container().id();
        builder.addRelationship(new Relationship(
                Ids.uses(unitId, storeId),
                unitId,
                storeId,
                "Reads from and writes to",
                "JDBC",
                List.of(evidence)));
    }

    /**
     * Resolves {@code ${VAR:default}} to its default. A placeholder without a default
     * is supplied only at deployment time — the value cannot be resolved statically
     * (Constraint 2 of the thesis requirements), so the URL is skipped with a warning.
     * Representing such relationships as explicitly unresolved is M2 scope (FR-RE-2).
     */
    private Optional<String> resolvePlaceholders(String url, String source, Diagnostics diagnostics) {
        Matcher matcher = PLACEHOLDER.matcher(url);
        StringBuilder resolved = new StringBuilder();
        while (matcher.find()) {
            if (matcher.group(2) == null) {
                diagnostics.warn("spring-datastore: datasource URL in " + source
                        + " uses placeholder ${" + matcher.group(1)
                        + "} without a default; cannot resolve statically, skipping");
                return Optional.empty();
            }
            matcher.appendReplacement(resolved, Matcher.quoteReplacement(matcher.group(2)));
        }
        matcher.appendTail(resolved);
        return Optional.of(resolved.toString());
    }
}