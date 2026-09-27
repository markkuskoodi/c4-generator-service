package ee.markkuskoodi.c4generator.export;

import ee.markkuskoodi.c4generator.model.Model;

/**
 * A pluggable rendering exporter (design D2). Exporters take the canonical model as
 * their only input — they never re-analyze the repository — so rendering formats can
 * be swapped without touching extraction (NFR-5).
 */
public interface Exporter {

    /** File name of the exported artifact, e.g. {@code workspace.dsl}. */
    String fileName();

    String export(Model model);
}