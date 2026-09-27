package ee.markkuskoodi.c4generator.model;

import java.util.Locale;

/**
 * Stable element identifiers (NFR-3, design D8): typed, hierarchical, derived from
 * build and configuration coordinates. Never derived from filesystem paths, counters,
 * timestamps, or iteration order.
 */
public final class Ids {

    private Ids() {
    }

    public static String system(String projectName) {
        return "system:" + slug(projectName);
    }

    /** Maven: groupId + artifactId. Gradle: group + settings-file project name. */
    public static String container(String group, String name) {
        return "container:" + group + ":" + name;
    }

    public static String dataStore(String product, String databaseName) {
        return "datastore:" + slug(product) + ":" + slug(databaseName);
    }

    public static String uses(String sourceId, String targetId) {
        return sourceId + "--uses--" + targetId;
    }

    /** Lowercase, non-alphanumerics collapsed to single dashes, trimmed. */
    static String slug(String value) {
        String slug = value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        if (slug.isEmpty()) {
            throw new IllegalArgumentException("Cannot derive an identifier from: '" + value + "'");
        }
        return slug;
    }
}