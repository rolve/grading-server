package ch.trick17.gradingserver.webapp;

import org.h2.tools.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.SQLException;

import static org.h2.tools.Server.createTcpServer;

@Configuration
public class H2ServerConfig {

    private final int dbPort;

    public H2ServerConfig(WebAppProperties props) {
        dbPort = props.getDbPort();
    }

    @Bean
    public Server server() throws SQLException {
        return createTcpServer("-tcp", "-tcpAllowOthers",
                "-tcpPort", Integer.toString(dbPort)).start();
    }
}
