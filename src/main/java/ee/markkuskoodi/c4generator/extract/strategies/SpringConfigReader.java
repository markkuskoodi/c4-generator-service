package ee.markkuskoodi.c4generator.extract.strategies;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/** Flattens Spring configuration files into dot-separated key/value pairs. */
final class SpringConfigReader {

    private SpringConfigReader() {
    }

    /** Sorted map for deterministic iteration. */
    static Map<String, String> readProperties(Reader reader) throws IOException {
        Properties properties = new Properties();
        properties.load(reader);
        Map<String, String> flat = new TreeMap<>();
        for (String name : properties.stringPropertyNames()) {
            flat.put(name, properties.getProperty(name));
        }
        return flat;
    }

    /** Reads all YAML documents in the file ('---' separated profiles) and flattens them. */
    static Map<String, String> readYaml(Reader reader) {
        Map<String, String> flat = new TreeMap<>();
        for (Object document : new Yaml().loadAll(reader)) {
            if (document instanceof Map<?, ?> map) {
                flatten("", map, flat);
            }
        }
        return flat;
    }

    private static void flatten(String prefix, Map<?, ?> map, Map<String, String> out) {
        for (Map.Entry<?, ?> entry : new LinkedHashMap<>(map).entrySet()) {
            String key = prefix.isEmpty()
                    ? String.valueOf(entry.getKey())
                    : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                flatten(key, nested, out);
            } else if (value != null) {
                out.put(key, String.valueOf(value));
            }
        }
    }
}