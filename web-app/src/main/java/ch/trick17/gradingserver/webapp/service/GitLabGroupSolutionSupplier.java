package ch.trick17.gradingserver.webapp.service;

import ch.trick17.gradingserver.webapp.controller.WebhooksController;
import ch.trick17.gradingserver.webapp.model.Solution;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.models.Event;
import org.gitlab4j.api.models.Member;
import org.gitlab4j.api.models.ProjectHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.lang.String.join;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;
import static org.gitlab4j.api.Constants.ActionType.PUSHED;
import static org.gitlab4j.api.models.AccessLevel.DEVELOPER;

public class GitLabGroupSolutionSupplier implements SolutionSupplier<GitLabApiException> {

    private static final Logger logger = LoggerFactory.getLogger(GitLabGroupSolutionSupplier.class);

    private final String hostUrl;
    private final String token;
    private final String groupPath;

    private AccessLevel minAccessLevel = DEVELOPER;
    private boolean ignoringCommonMembers = true;
    private String webhookBaseUrl = null;

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

    public String getWebhookBaseUrl() {
        return webhookBaseUrl;
    }

    public void setWebhookBaseUrl(String webhookBaseUrl) {
        this.webhookBaseUrl = webhookBaseUrl;
    }

    @Override
    public List<SolutionInfo> get(Collection<Solution> existing) throws GitLabApiException {
        var existingRepos = existing.stream()
                .map(Solution::getRepoUrl)
                .collect(toSet());
        try (var api = new GitLabApi(hostUrl, token)) {
            var projects = api.getGroupApi().getProjects(groupPath);

            projects.removeIf(p -> existingRepos.contains(p.getHttpUrlToRepo()));
            logger.info("{} new GitLab projects found", projects.size());
            if (projects.isEmpty()) {
                return emptyList();
            }

            if (webhookBaseUrl != null) {
                var url = webhookBaseUrl + WebhooksController.GITLAB_PUSH_PATH;
                var enabledHooks = new ProjectHook();
                enabledHooks.setPushEvents(true);
                var sslEnabled = url.startsWith("https");
                logger.info("Adding push webhooks with URL {}", url);
                var added = 0;
                for (var project : projects) {
                    var hooks = api.getProjectApi().getHooks(project);
                    if (hooks.stream().noneMatch(h -> h.getUrl().equals(url))) {
                        api.getProjectApi().addHook(project, url, enabledHooks, sslEnabled, null);
                        added++;
                    }
                }
                logger.info("{} new hooks added", added);
            }

            var authors = new ArrayList<Set<String>>();
            for (var project : projects) {
                var members = api.getProjectApi().getMembers(project);
                authors.add(members.stream()
                        .filter(m -> m.getAccessLevel().compareTo(minAccessLevel) >= 0)
                        .map(Member::getUsername)
                        .collect(toCollection(HashSet::new)));
            }

            var ignoredPushUsers = new HashSet<String>();
            if (ignoringCommonMembers) {
                var common = authors.stream().skip(1)
                        .collect(() -> new HashSet<>(authors.get(0)),
                                Set::retainAll, Set::retainAll);
                if (!common.isEmpty()) {
                    logger.info("Ignoring authorship from common project members: {}",
                            join(", ", common));
                    authors.forEach(s -> s.removeAll(common));
                    ignoredPushUsers.addAll(common);
                }
            }

            api.getGroupApi().getMembers(groupPath).stream()
                    .map(Member::getUsername)
                    .forEach(ignoredPushUsers::add);
            logger.info("Ignoring initial pushes from users: {}",
                    join(", ", ignoredPushUsers));
            var sols = new ArrayList<SolutionInfo>();
            for (int i = 0, projectsSize = projects.size(); i < projectsSize; i++) {
                var pushes = api.getEventsApi().getProjectEvents(projects.get(i),
                        PUSHED, null, null, null, null);
                var ignoredInitialCommit = pushes.stream()
                        .sorted(comparing(Event::getCreatedAt))
                        .takeWhile(p -> ignoredPushUsers.contains(p.getAuthorUsername()))
                        .map(p -> p.getPushData().getCommitTo())
                        .reduce((first, second) -> second) // last
                        .orElse(null);
                var repoUrl = projects.get(i).getHttpUrlToRepo();
                if (ignoredInitialCommit != null) {
                    logger.debug("Ignoring initial commit {} for {}", ignoredInitialCommit, repoUrl);
                }
                sols.add(new SolutionInfo(repoUrl, authors.get(i), ignoredInitialCommit));
            }
            return sols;
        }
    }

    @Override
    public String toString() {
        return "GitLab group " + hostUrl + "/" + groupPath;
    }
}
