package ch.trick17.gradingserver.gradingservice;

import ch.trick17.jtt.grader.Grader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GradingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GradingServiceApplication.class, args);
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
