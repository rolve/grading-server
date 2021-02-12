package ch.trick17.gradingserver.webapp.service;

import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Service
public class LatestCommitFetcher {

    public Optional<String> fetchLatestCommit(String repoUrl, String token) throws GitAPIException, IOException {
        var repo = new InMemoryRepository(new DfsRepositoryDescription());
        var ref = new LsRemoteCommand(repo)
                .setRemote(repoUrl)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider("", token))
                .callAsMap()
                .get("refs/heads/master");
        return Optional.ofNullable(ref).map(Ref::getObjectId).map(ObjectId::getName);
    }
}
