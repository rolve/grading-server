package ch.trick17.gradingserver.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.eclipse.jgit.lib.ObjectId.fromString;
import static org.eclipse.jgit.util.FileUtils.*;

@BenchmarkMode(Mode.SingleShotTime)
@Fork(1)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@OutputTimeUnit(MILLISECONDS)
@State(Scope.Benchmark)
public class CodeDownloadBenchmark {

    static final String REPO = "https://github.com/rolve/java-teaching-tools.git";
    static final String COMMIT = "6493a0fe36f3739929a981ce1440111d0071e08e";

    @Benchmark
    public Map<String, byte[]> viaDisk() throws IOException, GitAPIException {
        var dir = Path.of("test-temp");
        var clone = Git.cloneRepository()
                .setURI(REPO)
                .setDirectory(dir.toFile());
        try (var git = clone.call()) {
            git.checkout().setName(COMMIT).call();
        }
        var result = new HashMap<String, byte[]>();
        try (var walk = Files.walk(dir).filter(Files::isRegularFile)) {
            walk.forEach(p -> {
                try {
                    result.put(p.toString(), Files.readAllBytes(p));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        delete(dir.toFile(), RECURSIVE | RETRY);
        return result;
    }

    @Benchmark
    public Map<String, byte[]> inMemory() throws IOException, GitAPIException {
        try (var repo = new InMemoryRepository(new DfsRepositoryDescription())) {
            var git = new Git(repo);
            git.fetch()
                    .setRemote(REPO)
                    .setRefSpecs(COMMIT)
                    .call();
            try (var revWalk = new RevWalk(repo)) {
                var tree = revWalk.parseCommit(fromString(COMMIT)).getTree();
                var treeWalk = new TreeWalk(repo);
                treeWalk.addTree(tree);
                treeWalk.setRecursive(true);
                var result = new HashMap<String, byte[]>();
                while (treeWalk.next()) {
                    var objectId = treeWalk.getObjectId(0);
                    var objectLoader = repo.open(objectId);
                    result.put(treeWalk.getPathString(), objectLoader.getBytes());
                }
                return result;
            }
        }
    }

    public static void main(String[] args) throws IOException {
        org.openjdk.jmh.Main.main(new String[] {CodeDownloadBenchmark.class.getName()});
    }
}
