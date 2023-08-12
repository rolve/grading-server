package ch.trick17.gradingserver.webapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;

import java.util.concurrent.Executor;

import static java.util.concurrent.Executors.newCachedThreadPool;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties(WebAppProperties.class)
@EntityScan("ch.trick17.gradingserver")
public class GradingServerWebApp {

    public static void main(String[] args) {
        SpringApplication.run(GradingServerWebApp.class, args);
    }

    @Bean
    public Executor taskExecutor() {
        return newCachedThreadPool();
    }
}
