package ch.trick17.gradingserver;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("grading-server")
public class GradingServerProperties {

    private String defaultGitLabHost;
    private String testRunnerVmArgs = "";

    public String getDefaultGitLabHost() {
        return defaultGitLabHost;
    }

    public void setDefaultGitLabHost(String defaultGitLabHost) {
        this.defaultGitLabHost = defaultGitLabHost;
    }

    public String getTestRunnerVmArgs() {
        return testRunnerVmArgs;
    }

    public void setTestRunnerVmArgs(String testRunnerVmArgs) {
        this.testRunnerVmArgs = testRunnerVmArgs;
    }
}
