package ch.trick17.gradingserver.webapp.service;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.models.Member;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;
import static org.gitlab4j.api.models.AccessLevel.DEVELOPER;

public class GitLabGroupSolutionSupplier implements SolutionSupplier {

    private final String hostUrl;
    private final String token;
    private final String groupPath;

    private AccessLevel minAccessLevel = DEVELOPER;
    private boolean ignoringCommonMembers = true;

    public GitLabGroupSolutionSupplier(String hostUrl, String groupPath, String token) {
        this.hostUrl = requireNonNull(hostUrl);
        this.token = requireNonNull(token);
        this.groupPath = requireNonNull(groupPath);
    }

    public AccessLevel getMinAccessLevel() {
        return minAccessLevel;
    }

    /**
     * Defines the minimum {@link AccessLevel} that a member of a
     * GitLab project needs to have to be considered an author. The
     * default is DEVELOPER.
     */
    public void setMinAccessLevel(AccessLevel minAccessLevel) {
        this.minAccessLevel = minAccessLevel;
    }

    public boolean isIgnoringCommonMembers() {
        return ignoringCommonMembers;
    }

    /**
     * Defines whether members of all projects in the group are
     * ignored, i.e., not considered authors. The default is
     * <code>true</code>, as it is expected that this applies to
     * instructors.
     */
    public void setIgnoringCommonMembers(boolean ignoringCommonMembers) {
        this.ignoringCommonMembers = ignoringCommonMembers;
    }

    @Override
    public List<SolutionInfo> get() throws GitLabApiException {
        try (GitLabApi api = new GitLabApi(hostUrl, token)) {
            var projects = api.getGroupApi().getProjects(groupPath);
            if (projects.isEmpty()) {
                return emptyList();
            }
            var result = new ArrayList<SolutionInfo>();
            for (var project : projects) {
                var members = api.getProjectApi().getMembers(project);
                var authorNames = members.stream()
                        .filter(m -> m.getAccessLevel().compareTo(minAccessLevel) >= 0)
                        .map(Member::getUsername)
                        .collect(toCollection(HashSet::new));
                result.add(new SolutionInfo(project.getHttpUrlToRepo(), authorNames));
            }
            if (ignoringCommonMembers) {
                var common = result.stream().skip(1)
                        .map(SolutionInfo::authorNames)
                        .collect(() -> new HashSet<>(result.get(0).authorNames()), Set::retainAll, Set::retainAll);
                result.stream()
                        .map(SolutionInfo::authorNames)
                        .forEach(s -> s.removeAll(common));
            }
            return result;
        }
    }
}
