package ch.trick17.gradingserver.webapp;

import org.h2.tools.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.SQLException;

import static org.h2.tools.Server.createTcpServer;

@Configuration
public class H2ServerConfig {

    private final boolean enabled;
    private final int port;

    public H2ServerConfig(WebAppProperties props) {
        enabled = props.getDbServer().isEnabled();
        port = props.getDbServer().getPort();
    }

    @Bean
    public Server server() throws SQLException {
        if (enabled) {
            return createTcpServer("-tcp", "-tcpAllowOthers",
                    "-tcpPort", Integer.toString(port)).start();
        } else {
            return null;
        }
    }
}
