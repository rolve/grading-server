package ch.trick17.gradingserver.gradingservice.controller;

import ch.trick17.gradingserver.gradingservice.model.JobRunner;
import ch.trick17.gradingserver.gradingservice.model.GradingJob;
import ch.trick17.gradingserver.gradingservice.model.GradingJobRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Optional;

import static org.springframework.http.ResponseEntity.created;

@RestController
@RequestMapping(path = "/api/v1/grading-jobs")
public class GradingJobController {

    private final GradingJobRepository repo;
    private final JobRunner jobRunner;

    public GradingJobController(GradingJobRepository repo, JobRunner jobRunner) {
        this.repo = repo;
        this.jobRunner = jobRunner;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody GradingJob job) {
        if (job.hasResult()) {
            throw new IllegalArgumentException();
        }
        repo.save(job);
        jobRunner.submit(job);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .pathSegment(job.getId()).build().toUri();
        return created(location).build();
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<GradingJob> get(@PathVariable String id) {
        return ResponseEntity.of(repo.findById(id));
    }
}
