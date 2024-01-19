package ch.trick17.gradingserver.webapp;

import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.SQLException;

import static org.h2.tools.Server.createTcpServer;

@Configuration
public class H2ServerConfig {

    private static final Logger logger = LoggerFactory.getLogger(H2ServerConfig .class);

    private final boolean enabled;
    private final int port;

    public H2ServerConfig(WebAppProperties props) {
        enabled = props.getDbServer().isEnabled();
        port = props.getDbServer().getPort();
    }

    @Bean
    public Server server() {
        if (enabled) {
            try {
                var server = createTcpServer("-tcp", "-tcpAllowOthers",
                        "-tcpPort", Integer.toString(port)).start();
                logger.info("Started H2 TCP server on port {}", port);
                return server;
            } catch (SQLException e) {
                logger.error("Could not start H2 TCP server", e);
            }
        }
        return null;
    }
}
