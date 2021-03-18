package ch.trick17.gradingserver.webapp.service;

import ch.trick17.gradingserver.webapp.controller.WebhooksController;
import ch.trick17.gradingserver.webapp.model.Solution;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.lang.String.join;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;
import static org.gitlab4j.api.Constants.ActionType.PUSHED;
import static org.gitlab4j.api.Constants.SortOrder.DESC;
import static org.gitlab4j.api.models.AccessLevel.DEVELOPER;

public class GitLabGroupSolutionSupplier implements SolutionSupplier<GitLabApiException> {

    private static final Logger logger = LoggerFactory.getLogger(GitLabGroupSolutionSupplier.class);

    private final String hostUrl;
    private final String token;
    private final String groupPath;

    private AccessLevel minAccessLevel = DEVELOPER;
    private boolean ignoringCommonMembers = true;
    private boolean ignoringAuthorless = true;
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

    public boolean isIgnoringAuthorless() {
        return ignoringAuthorless;
    }

    public void setIgnoringAuthorless(boolean ignoringAuthorless) {
        this.ignoringAuthorless = ignoringAuthorless;
    }

    public String getWebhookBaseUrl() {
        return webhookBaseUrl;
    }

    public void setWebhookBaseUrl(String webhookBaseUrl) {
        this.webhookBaseUrl = webhookBaseUrl;
    }

    @Override
    public List<NewSolution> get(Collection<Solution> existing) throws GitLabApiException {
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

            var authors = new ArrayList<Set<String>>();
            for (var project : projects) {
                var members = api.getProjectApi().getMembers(project);
                authors.add(members.stream()
                        .filter(m -> m.getAccessLevel().compareTo(minAccessLevel) >= 0)
                        .map(Member::getUsername)
                        .collect(toCollection(HashSet::new)));
            }

            var ignoredPushers = new HashSet<String>();
            if (ignoringCommonMembers) {
                var common = authors.stream().skip(1)
                        .collect(() -> new HashSet<>(authors.get(0)),
                                Set::retainAll, Set::retainAll);
                if (!common.isEmpty()) {
                    logger.info("Ignoring authorship from common project members: {}",
                            join(", ", common));
                    authors.forEach(s -> s.removeAll(common));
                    ignoredPushers.addAll(common);
                }
            }

            if (ignoringAuthorless) {
                for (int i = 0; i < projects.size(); i++) {
                    if (authors.get(i).isEmpty()) {
                        logger.info("Ignoring authorless project: {}", projects.get(i).getName());
                        authors.remove(i);
                        projects.remove(i);
                        i--;
                    }
                }
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

            api.getGroupApi().getMembers(groupPath).stream()
                    .map(Member::getUsername)
                    .forEach(ignoredPushers::add);
            logger.info("Ignoring pushes from users: {}", join(", ", ignoredPushers));

            var sols = new ArrayList<NewSolution>();
            for (int i = 0; i < projects.size(); i++) {
                var repoUrl = projects.get(i).getHttpUrlToRepo();
                var pushEvents = api.getEventsApi().getProjectEvents(projects.get(i), PUSHED,
                        null, null, null, DESC);
                var initialCommit = pushEvents.stream()
                        .filter(e -> !ignoredPushers.contains(e.getAuthorUsername()))
                        .map(Event::getPushData)
                        .filter(p -> p.getRef().equals("master"))
                        .map(PushData::getCommitTo)
                        .findFirst().orElse(null);
                sols.add(new NewSolution(repoUrl, authors.get(i), ignoredPushers, initialCommit));
            }
            return sols;
        }
    }

    @Override
    public String toString() {
        return "GitLab group " + hostUrl + "/" + groupPath;
    }
}
