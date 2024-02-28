package ch.trick17.gradingserver;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

import static java.util.Collections.emptyList;

@ConfigurationProperties("grading-server")
public class GradingServerProperties {

    private DbServer dbServer;
    private String defaultGitLabHost;
    private List<String> testRunnerVmArgs = emptyList();

    public DbServer getDbServer() {
        return dbServer;
    }

    public void setDbServer(DbServer dbServer) {
        this.dbServer = dbServer;
    }

    public String getDefaultGitLabHost() {
        return defaultGitLabHost;
    }

    public void setDefaultGitLabHost(String defaultGitLabHost) {
        this.defaultGitLabHost = defaultGitLabHost;
    }

    public List<String> getTestRunnerVmArgs() {
        return testRunnerVmArgs;
    }

    public void setTestRunnerVmArgs(List<String> testRunnerVmArgs) {
        this.testRunnerVmArgs = testRunnerVmArgs;
    }

    public static class DbServer {

        private boolean enabled = false;
        private int port = -1;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }
}
