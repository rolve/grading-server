package ch.trick17.gradingserver.webapp;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("grading-server.web-app")
public class WebAppProperties {

    private String gradingServiceHost;
    private int gradingServicePort;

    private int dbPort;

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

    public int getDbPort() {
        return dbPort;
    }

    public void setDbPort(int dbPort) {
        this.dbPort = dbPort;
    }
}
