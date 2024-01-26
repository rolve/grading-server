package ch.trick17.gradingserver.webapp;

import ch.trick17.jtt.grader.Grader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
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
public class GradingServerWebApp {

    public static void main(String[] args) {
        SpringApplication.run(GradingServerWebApp.class, args);
    }

    @Bean
    public Executor taskExecutor() {
        return newCachedThreadPool();
    }

    @Bean
    public Grader grader() {
        var grader = new Grader();
        grader.setLogDir(null);
        grader.setResultsDir(null);
        grader.setParallelism(1); // no need for parallelism, grading one at a time
        return grader;
    }
}
