package ee.markkuskoodi.c4generator.extract;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects warnings emitted during extraction (NFR-7: warn and continue).
 * Warnings go to the user via stderr, never into the model — the model must stay
 * deterministic and carry only derived facts.
 */
public final class Diagnostics {

    private final List<String> warnings = new ArrayList<>();

    public void warn(String message) {
        warnings.add(message);
    }

    public List<String> warnings() {
        return List.copyOf(warnings);
    }
}