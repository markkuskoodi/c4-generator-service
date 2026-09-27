package ee.markkuskoodi.c4generator.extract.strategies;

import java.util.Map;
import java.util.Optional;

/** Derives data-store product and database name from a JDBC URL (design D6). */
final class JdbcUrls {

    record DataStoreRef(String product, String database) {
    }

    private static final Map<String, String> PRODUCTS = Map.of(
            "mysql", "MySQL",
            "mariadb", "MariaDB",
            "postgresql", "PostgreSQL",
            "h2", "H2",
            "hsqldb", "HSQLDB",
            "oracle", "Oracle",
            "sqlserver", "SQL Server",
            "db2", "Db2",
            "sqlite", "SQLite");

    private JdbcUrls() {
    }

    static Optional<DataStoreRef> parse(String url) {
        if (url == null || !url.startsWith("jdbc:")) {
            return Optional.empty();
        }
        String rest = url.substring("jdbc:".length());
        int colon = rest.indexOf(':');
        if (colon <= 0) {
            return Optional.empty();
        }
        String subprotocol = rest.substring(0, colon).toLowerCase();
        String product = PRODUCTS.get(subprotocol);
        if (product == null) {
            product = subprotocol.substring(0, 1).toUpperCase() + subprotocol.substring(1);
        }
        String database = databaseName(subprotocol, rest.substring(colon + 1));
        return Optional.of(new DataStoreRef(product, database.isBlank() ? "default" : database));
    }

    private static String databaseName(String subprotocol, String detail) {
        // strip parameters: '?key=…' (URL style) and ';key=…' (H2/SQLServer style)
        String cleaned = detail;
        for (char separator : new char[]{'?', ';'}) {
            int idx = cleaned.indexOf(separator);
            if (idx >= 0) {
                cleaned = cleaned.substring(0, idx);
            }
        }
        if (subprotocol.equals("h2")) {
            // forms: mem:name | file:/path/name | tcp://host[:port]/name
            String h2 = cleaned;
            for (String prefix : new String[]{"mem:", "file:", "tcp://", "ssl://"}) {
                if (h2.startsWith(prefix)) {
                    h2 = h2.substring(prefix.length());
                }
            }
            int slash = h2.lastIndexOf('/');
            return slash >= 0 ? h2.substring(slash + 1) : h2;
        }
        // generic form: //host[:port]/database
        int slash = cleaned.lastIndexOf('/');
        return slash >= 0 ? cleaned.substring(slash + 1) : "";
    }
}