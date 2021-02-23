package ch.trick17.gradingserver.webapp.service;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public interface SolutionSupplier<E extends Exception> {

    List<SolutionInfo> get() throws E;

    record SolutionInfo(String repoUrl, Set<String> authorNames, String ignoredInitialCommit) {

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (o == null || getClass() != o.getClass()) {
                return false;
            }
            return repoUrl.equals(((SolutionInfo) o).repoUrl);
        }

        @Override
        public int hashCode() {
            return Objects.hash(repoUrl);
        }
    }
}
