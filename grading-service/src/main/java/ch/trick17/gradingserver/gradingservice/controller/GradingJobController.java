package ch.trick17.gradingserver.gradingservice.controller;

import ch.trick17.gradingserver.GradingResult;
import ch.trick17.gradingserver.gradingservice.model.GradingJob;
import ch.trick17.gradingserver.gradingservice.model.GradingJobRepository;
import ch.trick17.gradingserver.gradingservice.service.JobRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Optional;

import static java.lang.Long.MAX_VALUE;
import static org.springframework.http.ResponseEntity.created;

@RestController
@RequestMapping("/api/v1/grading-jobs")
public class GradingJobController {

    private final GradingJobRepository repo;
    private final JobRunner jobRunner;

    public GradingJobController(GradingJobRepository repo, JobRunner jobRunner) {
        this.repo = repo;
        this.jobRunner = jobRunner;
    }

    @PostMapping
    public DeferredResult<ResponseEntity<GradingJob>> create(@RequestBody GradingJob job,
                @RequestParam(required = false, defaultValue = "false") boolean waitUntilDone) {
        if (job.hasResult()) {
            throw new IllegalArgumentException();
        }
        repo.save(job);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .pathSegment(job.getId()).build().toUri();

        var deferred = new DeferredResult<ResponseEntity<GradingJob>>(MAX_VALUE);
        Runnable setResult = () -> deferred.setResult(created(location).body(job));
        if (waitUntilDone) {
            jobRunner.submit(job, setResult);
        } else {
            jobRunner.submit(job);
            setResult.run();
        }
        return deferred;
    }

    @GetMapping("/{id}")
    public ResponseEntity<GradingJob> get(@PathVariable String id) {
        return ResponseEntity.of(repo.findById(id));
    }

    @GetMapping("/{id}/result")
    public ResponseEntity<GradingResult> getResult(@PathVariable String id) {
        var result = repo.findById(id)
                .flatMap(job -> Optional.ofNullable(job.getResult()));
        return ResponseEntity.of(result);
    }
}
