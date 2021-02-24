package ch.trick17.gradingserver.webapp;

import org.ocpsoft.prettytime.PrettyTime;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(WebAppProperties.class)
public class GradingServerWebApp {

    public static void main(String[] args) {
        SpringApplication.run(GradingServerWebApp.class, args);
    }

    @Bean
    public Executor taskExecutor() {
        return Executors.newCachedThreadPool();
    }

    @Bean
    public Supplier<PrettyTime> prettyTime() {
        return PrettyTime::new;
    }
}
