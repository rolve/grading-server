package ch.trick17.gradingserver.gradingservice.model;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class JobIdGeneratorTest {

    @Autowired
    private GradingJobRepository repo;

    @Test
    void generate() {
        var ids = new HashSet<String>();
        for (int i = 0; i < 1000; i++) {
            var job = new GradingJob();
            repo.save(job);
            assertEquals(32, job.getId().length());
            ids.add(job.getId());
        }
        assertEquals(1000, ids.size());
    }
}
