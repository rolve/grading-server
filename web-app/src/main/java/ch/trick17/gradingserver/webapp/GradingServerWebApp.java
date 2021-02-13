package ch.trick17.gradingserver.webapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class GradingServerWebApp {

    public static void main(String[] args) {
        SpringApplication.run(GradingServerWebApp.class, args);
    }

}
