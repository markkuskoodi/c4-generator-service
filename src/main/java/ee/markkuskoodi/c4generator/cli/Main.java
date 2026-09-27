package ee.markkuskoodi.c4generator.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "c4-generator-service",
        description = "Generates C4 architecture models from a software system's artifacts.",
        subcommands = {GenerateCommand.class},
        mixinStandardHelpOptions = true,
        version = "c4-generator-service 0.1.0")
public final class Main implements Runnable {

    @picocli.CommandLine.Spec
    private picocli.CommandLine.Model.CommandSpec spec;

    /** Invoked with no arguments: print usage (spec: cli / Usage help). */
    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }
}