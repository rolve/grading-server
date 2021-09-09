package ch.trick17.gradingserver.webapp;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("grading-server.web-app")
public class WebAppProperties {

    private String gradingServiceHost;
    private int gradingServicePort;

    private DbServer dbServer;

    private String defaultGitLabHost;

    public String getGradingServiceHost() {
        return gradingServiceHost;
    }

    public void setGradingServiceHost(String gradingServiceHost) {
        this.gradingServiceHost = gradingServiceHost;
    }

    public int getGradingServicePort() {
        return gradingServicePort;
    }

    public void setGradingServicePort(int gradingServicePort) {
        this.gradingServicePort = gradingServicePort;
    }

    public String getGradingServiceBaseUrl() {
        return "http://" + gradingServiceHost + ":" + gradingServicePort;
    }

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
