package ch.trick17.gradingserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.Executor;

import static java.util.concurrent.Executors.newCachedThreadPool;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(GradingServerProperties.class)
public class GradingServer {

    public static void main(String[] args) {
        SpringApplication.run(GradingServer.class, args);
    }

    @Bean
    public Executor taskExecutor() {
        return newCachedThreadPool();
    }
}
