package ee.markkuskoodi.c4generator.cli;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class GenerateCommandE2eTest {

    private static final Path FIXTURE = Path.of("src/test/resources/fixtures/maven-app");

    private record CliRun(int exitCode, String out, String err) {
    }

    private CliRun runCli(String... args) {
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        int exitCode = new CommandLine(new Main())
                .setOut(new PrintWriter(out))
                .setErr(new PrintWriter(err))
                .execute(args);
        return new CliRun(exitCode, out.toString(), err.toString());
    }

    @Test
    void generatesModelAndWorkspaceWithoutTouchingTheRepository(@TempDir Path dir) throws Exception {
        Path repo = dir.resolve("repo");
        String sha = createFixtureRepo(repo);
        Path definition = Files.writeString(dir.resolve("project.yaml"), """
                name: Fixture System
                repositories:
                  - path: repo
                """);
        Path output = dir.resolve("out");
        Map<String, String> before = workingTreeDigest(repo);

        CliRun run = runCli("generate", definition.toString(), "-o", output.toString());

        assertThat(run.exitCode()).as("stderr: " + run.err()).isZero();
        assertThat(output.resolve("model.json")).exists();
        assertThat(output.resolve("workspace.dsl")).exists();
        assertThat(Files.readString(output.resolve("model.json"))).contains(sha);
        // FR-IN-5: the analyzed repository's working tree is byte-identical
        assertThat(workingTreeDigest(repo)).isEqualTo(before);

        // NFR-2: a second run produces byte-identical outputs
        String model1 = Files.readString(output.resolve("model.json"));
        String dsl1 = Files.readString(output.resolve("workspace.dsl"));
        Path output2 = dir.resolve("out2");
        assertThat(runCli("generate", definition.toString(), "-o", output2.toString()).exitCode()).isZero();
        assertThat(Files.readString(output2.resolve("model.json"))).isEqualTo(model1);
        assertThat(Files.readString(output2.resolve("workspace.dsl"))).isEqualTo(dsl1);
    }

    @Test
    void failuresExitNonZeroWithMessageOnStderr(@TempDir Path dir) {
        CliRun run = runCli("generate", dir.resolve("missing.yaml").toString(), "-o", dir.resolve("out").toString());
        assertThat(run.exitCode()).isNotZero();
        assertThat(run.err()).contains("Error:").contains("missing.yaml");
    }

    @Test
    void helpPrintsUsageAndExitsZero() {
        CliRun run = runCli("--help");
        assertThat(run.exitCode()).isZero();
        assertThat(run.out()).contains("generate");
    }

    @Test
    void noArgumentsPrintsUsage() {
        CliRun run = runCli();
        assertThat(run.exitCode()).isZero();
        assertThat(run.out()).contains("Usage");
    }

    private String createFixtureRepo(Path repo) throws Exception {
        try (Stream<Path> files = Files.walk(FIXTURE)) {
            for (Path source : files.sorted().toList()) {
                Path target = repo.resolve(FIXTURE.relativize(source).toString());
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(source, target);
                }
            }
        }
        try (Git git = Git.init().setDirectory(repo.toFile()).call()) {
            git.add().addFilepattern(".").call();
            return git.commit().setMessage("init")
                    .setAuthor("Test", "test@example.com")
                    .setCommitter("Test", "test@example.com")
                    .setSign(false).call().getName();
        }
    }

    /** SHA-256 of every working-tree file (excluding .git), keyed by relative path. */
    private Map<String, String> workingTreeDigest(Path repo) throws IOException {
        Map<String, String> digests = new TreeMap<>();
        try (Stream<Path> files = Files.walk(repo)) {
            for (Path file : files.filter(Files::isRegularFile).sorted().toList()) {
                String relative = repo.relativize(file).toString().replace('\\', '/');
                if (relative.startsWith(".git/")) {
                    continue;
                }
                digests.put(relative, sha256(file));
            }
        }
        return digests;
    }

    private String sha256(Path file) throws IOException {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}