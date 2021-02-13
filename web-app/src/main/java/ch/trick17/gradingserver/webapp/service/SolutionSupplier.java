package ch.trick17.gradingserver.webapp.service;

import java.util.List;
import java.util.Set;

public interface SolutionSupplier<E extends Exception> {

    List<SolutionInfo> get() throws E;

    record SolutionInfo(String repoUrl, Set<String> authorNames) {}
}
