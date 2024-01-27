package ch.trick17.gradingserver.service;

import ch.trick17.gradingserver.model.Solution;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public interface SolutionSupplier<E extends Exception> {

    List<NewSolution> get(Collection<Solution> existing) throws E, GitAPIException;

    record NewSolution(
            String repoUrl,
            String branch,
            Set<String> authorNames,
            Set<String> ignoredPushers,
            String latestCommitHash) {

        @Override
        public boolean equals(Object other) {
            return other instanceof SolutionSupplier.NewSolution
                    && repoUrl.equals(((NewSolution) other).repoUrl);
        }

        @Override
        public int hashCode() {
            return Objects.hash(repoUrl);
        }
    }
}
