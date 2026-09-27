package ee.markkuskoodi.c4generator.cli;

import ee.markkuskoodi.c4generator.export.Exporter;
import ee.markkuskoodi.c4generator.export.StructurizrDslExporter;
import ee.markkuskoodi.c4generator.extract.Diagnostics;
import ee.markkuskoodi.c4generator.extract.ModelBuilder;
import ee.markkuskoodi.c4generator.extract.RepositoryContext;
import ee.markkuskoodi.c4generator.extract.StrategyRunner;
import ee.markkuskoodi.c4generator.ingest.IngestException;
import ee.markkuskoodi.c4generator.ingest.ProjectDefinition;
import ee.markkuskoodi.c4generator.ingest.ProjectDefinitionReader;
import ee.markkuskoodi.c4generator.ingest.RepositoryResolver;
import ee.markkuskoodi.c4generator.ingest.ResolvedRepository;
import ee.markkuskoodi.c4generator.model.AnalyzedRepository;
import ee.markkuskoodi.c4generator.model.Model;
import ee.markkuskoodi.c4generator.serialize.ModelSerializer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
        name = "generate",
        description = "Analyze the system described by a project definition and generate model.json and workspace.dsl.",
        mixinStandardHelpOptions = true)
public final class GenerateCommand implements Callable<Integer> {

    @Parameters(index = "0", paramLabel = "PROJECT_DEFINITION",
            description = "Path to the project definition YAML file.")
    private Path projectDefinition;

    @Option(names = {"-o", "--output"}, paramLabel = "DIR", defaultValue = "c4-output",
            description = "Output directory for model.json and workspace.dsl (default: ${DEFAULT-VALUE}).")
    private Path outputDir;

    @picocli.CommandLine.Spec
    private picocli.CommandLine.Model.CommandSpec spec;

    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        PrintWriter err = spec.commandLine().getErr();
        try {
            ProjectDefinition definition = new ProjectDefinitionReader().read(projectDefinition);
            Path definitionDir = projectDefinition.toAbsolutePath().normalize().getParent();

            ModelBuilder builder = new ModelBuilder(definition.name());
            Diagnostics diagnostics = new Diagnostics();
            RepositoryResolver resolver = new RepositoryResolver();
            StrategyRunner strategies = StrategyRunner.discover();

            for (ProjectDefinition.RepositoryRef ref : definition.repositories()) {
                ResolvedRepository repository = resolver.resolve(definitionDir, ref);
                builder.addRepository(new AnalyzedRepository(repository.ref(), repository.commitSha()));
                strategies.run(new RepositoryContext(repository.ref(), repository.rootDir()),
                        builder, diagnostics);
            }

            Model model = builder.build();
            Files.createDirectories(outputDir);
            new ModelSerializer().write(model, outputDir.resolve("model.json"));
            Exporter exporter = new StructurizrDslExporter();
            Files.writeString(outputDir.resolve(exporter.fileName()), exporter.export(model),
                    StandardCharsets.UTF_8);

            diagnostics.warnings().forEach(warning -> err.println("warning: " + warning));
            out.println("Generated " + outputDir.resolve("model.json") + " ("
                    + model.containers().size() + " containers, "
                    + model.relationships().size() + " relationships)");
            out.println("Generated " + outputDir.resolve(exporter.fileName()));
            return 0;
        } catch (IngestException e) {
            err.println("Error: " + e.getMessage());
            return 1;
        } catch (IOException e) {
            err.println("Error: cannot write output: " + e.getMessage());
            return 1;
        }
    }
}