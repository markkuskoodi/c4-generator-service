package ee.markkuskoodi.c4generator.ingest;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolves a repository reference from the project definition: relative paths are
 * resolved against the definition file's directory (design D10), the path must be a
 * Git repository working tree, and the HEAD commit SHA is read via JGit (design D7).
 */
public final class RepositoryResolver {

    public ResolvedRepository resolve(Path definitionDir, ProjectDefinition.RepositoryRef ref) {
        Path root = definitionDir.resolve(ref.path()).normalize();
        if (!Files.isDirectory(root)) {
            throw new IngestException("Repository path does not exist or is not a directory: " + root);
        }
        FileRepositoryBuilder builder = new FileRepositoryBuilder()
                .findGitDir(root.toFile())
                .setMustExist(true);
        if (builder.getGitDir() == null || !builder.getGitDir().toPath().normalize().startsWith(root)) {
            throw new IngestException("Not a Git repository: " + root);
        }
        try (Repository repository = builder.build()) {
            ObjectId head = repository.resolve("HEAD");
            if (head == null) {
                throw new IngestException("Repository has no commits (unborn HEAD): " + root);
            }
            return new ResolvedRepository(ref.path(), root, head.getName());
        } catch (IOException e) {
            throw new IngestException("Cannot read Git repository " + root + ": " + e.getMessage(), e);
        }
    }
}