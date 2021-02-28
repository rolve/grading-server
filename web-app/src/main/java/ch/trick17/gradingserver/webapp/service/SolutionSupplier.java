package ch.trick17.gradingserver.webapp.service;

import ch.trick17.gradingserver.webapp.model.Solution;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public interface SolutionSupplier<E extends Exception> {

    List<SolutionInfo> get(Collection<Solution> existing) throws E;

    record SolutionInfo(String repoUrl, Set<String> authorNames, String ignoredInitialCommit) {

        @Override
        public boolean equals(Object other) {
            return other instanceof SolutionInfo
                    && repoUrl.equals(((SolutionInfo) other).repoUrl);
        }

        @Override
        public int hashCode() {
            return Objects.hash(repoUrl);
        }
    }
}
