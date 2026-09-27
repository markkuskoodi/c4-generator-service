package ee.markkuskoodi.c4generator.extract.strategies;

import ee.markkuskoodi.c4generator.extract.Diagnostics;
import ee.markkuskoodi.c4generator.extract.ExtractionStrategy;
import ee.markkuskoodi.c4generator.extract.ModelBuilder;
import ee.markkuskoodi.c4generator.extract.RepositoryContext;
import ee.markkuskoodi.c4generator.model.Container;
import ee.markkuskoodi.c4generator.model.ContainerKind;
import ee.markkuskoodi.c4generator.model.Evidence;
import ee.markkuskoodi.c4generator.model.Ids;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Identifies a deployable unit from Gradle build scripts by static heuristics
 * (FR-UN-1, FR-UN-4, design D5b). Build scripts are programs, so they are never
 * executed; the heuristics read the {@code plugins} block for executable markers and
 * the settings file for stable coordinates. Anything uninterpretable produces a
 * warning and a partial result, never a failure (NFR-7). Root module only in M1.
 */
public final class GradleUnitStrategy implements ExtractionStrategy {

    private static final Pattern PLUGIN_ID = Pattern.compile(
            "id\\s*[(]?\\s*[\"']([A-Za-z0-9_.\\-]+)[\"']");
    private static final Pattern PLUGINS_BLOCK = Pattern.compile(
            "plugins\\s*\\{(.*?)}", Pattern.DOTALL);
    private static final Pattern BARE_PLUGIN = Pattern.compile(
            "(?m)^\\s*(application|war|java)\\s*$");
    private static final Pattern GROUP_ASSIGNMENT = Pattern.compile(
            "(?m)^\\s*group\\s*=?\\s*[\"']([^\"']+)[\"']");
    private static final Pattern ROOT_PROJECT_NAME = Pattern.compile(
            "rootProject\\.name\\s*=\\s*[\"']([^\"']+)[\"']");

    @Override
    public String id() {
        return "gradle-unit";
    }

    @Override
    public void extract(RepositoryContext context, ModelBuilder builder, Diagnostics diagnostics) {
        Path buildFile = firstExisting(context.rootDir(), "build.gradle", "build.gradle.kts");
        if (buildFile == null) {
            return;
        }
        String buildScript = readOrWarn(buildFile, context, diagnostics);
        if (buildScript == null) {
            return;
        }

        PluginScan plugins = scanPlugins(buildScript);
        if (!plugins.recognized()) {
            diagnostics.warn("gradle-unit: no plugins block interpretable in "
                    + context.relativize(buildFile) + "; skipping (see design D5b)");
            return;
        }
        if (!plugins.executable()) {
            return; // library module, not a deployable unit
        }

        String name = projectName(context, diagnostics);
        if (name == null) {
            return;
        }
        Matcher groupMatcher = GROUP_ASSIGNMENT.matcher(buildScript);
        String group = groupMatcher.find() ? groupMatcher.group(1) : name;

        String technology = plugins.springBoot() ? "Java, Spring Boot" : "Java";
        Container container = new Container(
                Ids.container(group, name),
                name,
                ContainerKind.APPLICATION,
                technology,
                List.of(new Evidence(context.relativize(buildFile))));
        builder.addUnit(container, context.rootDir());
    }

    private record PluginScan(boolean recognized, boolean springBoot, boolean executable) {
    }

    private PluginScan scanPlugins(String buildScript) {
        Matcher block = PLUGINS_BLOCK.matcher(buildScript);
        if (!block.find()) {
            return new PluginScan(false, false, false);
        }
        String body = block.group(1);
        boolean springBoot = false;
        boolean war = false;
        Matcher ids = PLUGIN_ID.matcher(body);
        while (ids.find()) {
            String pluginId = ids.group(1);
            springBoot |= pluginId.equals("org.springframework.boot");
            war |= pluginId.equals("war");
        }
        Matcher bare = BARE_PLUGIN.matcher(body);
        boolean application = false;
        while (bare.find()) {
            application |= bare.group(1).equals("application");
            war |= bare.group(1).equals("war");
        }
        application |= body.contains("id(\"application\")") || body.contains("id 'application'")
                || body.contains("id(\"war\")");
        return new PluginScan(true, springBoot, springBoot || application || war);
    }

    private String projectName(RepositoryContext context, Diagnostics diagnostics) {
        Path settings = firstExisting(context.rootDir(), "settings.gradle", "settings.gradle.kts");
        if (settings != null) {
            String content = readOrWarn(settings, context, diagnostics);
            if (content != null) {
                Matcher name = ROOT_PROJECT_NAME.matcher(content);
                if (name.find()) {
                    return name.group(1);
                }
            }
        }
        // Gradle defaults the project name to the directory name, which depends on the
        // checkout location and is therefore not a stable coordinate (NFR-3).
        String fallback = context.rootDir().getFileName().toString();
        diagnostics.warn("gradle-unit: no rootProject.name in settings file; falling back to "
                + "directory name '" + fallback + "', which is not checkout-stable (NFR-3)");
        return fallback;
    }

    private static Path firstExisting(Path dir, String... names) {
        for (String name : names) {
            Path candidate = dir.resolve(name);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static String readOrWarn(Path file, RepositoryContext context, Diagnostics diagnostics) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            diagnostics.warn("gradle-unit: cannot read " + context.relativize(file) + ": " + e.getMessage());
            return null;
        }
    }
}