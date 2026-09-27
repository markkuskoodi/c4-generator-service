package ee.markkuskoodi.c4generator.ingest;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RepositoryResolverTest {

    private final RepositoryResolver resolver = new RepositoryResolver();

    @Test
    void resolvesRepositoryAndReadsHeadSha(@TempDir Path dir) throws Exception {
        Path repoDir = Files.createDirectory(dir.resolve("repo"));
        RevCommit commit;
        try (Git git = Git.init().setDirectory(repoDir.toFile()).call()) {
            Files.writeString(repoDir.resolve("file.txt"), "hello\n");
            git.add().addFilepattern(".").call();
            commit = git.commit().setMessage("init")
                    .setAuthor("Test", "test@example.com")
                    .setCommitter("Test", "test@example.com")
                    .setSign(false).call();
        }

        ResolvedRepository resolved = resolver.resolve(dir, new ProjectDefinition.RepositoryRef("repo"));

        assertThat(resolved.commitSha()).isEqualTo(commit.getName()).hasSize(40);
        assertThat(resolved.rootDir()).isEqualTo(repoDir);
        assertThat(resolved.ref()).isEqualTo("repo");
    }

    @Test
    void nonRepositoryPathIsReported(@TempDir Path dir) throws Exception {
        Files.createDirectory(dir.resolve("plain"));
        assertThatThrownBy(() -> resolver.resolve(dir, new ProjectDefinition.RepositoryRef("plain")))
                .isInstanceOf(IngestException.class)
                .hasMessageContaining("Not a Git repository");
    }

    @Test
    void missingPathIsReported(@TempDir Path dir) {
        assertThatThrownBy(() -> resolver.resolve(dir, new ProjectDefinition.RepositoryRef("missing")))
                .isInstanceOf(IngestException.class)
                .hasMessageContaining("does not exist");
    }
}