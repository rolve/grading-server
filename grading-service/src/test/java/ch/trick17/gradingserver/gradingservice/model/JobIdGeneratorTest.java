package ch.trick17.gradingserver.gradingservice.model;

import ch.trick17.gradingserver.CodeLocation;
import ch.trick17.gradingserver.GradingConfig;
import ch.trick17.gradingserver.GradingOptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.HashSet;

import static ch.trick17.gradingserver.GradingConfig.ProjectStructure.ECLIPSE;
import static ch.trick17.gradingserver.GradingOptions.Compiler.JAVAC;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class JobIdGeneratorTest {

    @Autowired
    private GradingJobRepository repo;

    @Test
    void generate() {
        var ids = new HashSet<String>();
        for (int i = 0; i < 1000; i++) {
            var job = new GradingJob(new CodeLocation("", ""),
                    new GradingConfig("", "", ECLIPSE,
                            new GradingOptions(JAVAC, 3, Duration.ofSeconds(1), Duration.ofSeconds(1), true)));
            repo.save(job);
            assertEquals(32, job.getId().length());
            ids.add(job.getId());
        }
        assertEquals(1000, ids.size());
    }
}
