package ch.trick17.gradingserver.webapp.service;

import java.util.List;
import java.util.Set;

public interface SolutionSupplier {

    List<SolutionInfo> get() throws Exception;

    record SolutionInfo(String repoUrl, Set<String> authorNames) {}
}
