package ch.trick17.gradingserver.gradingservice.controller;

import ch.trick17.gradingserver.gradingservice.model.GradingJob;
import ch.trick17.gradingserver.gradingservice.model.GradingJobRepository;
import ch.trick17.gradingserver.gradingservice.service.JobRunner;
import ch.trick17.gradingserver.model.GradingResult;
import ch.trick17.gradingserver.model.JarFileRepository;
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
    private final JarFileRepository jarFileRepo;
    private final JobRunner jobRunner;

    public GradingJobController(GradingJobRepository repo,
                                JarFileRepository jarFileRepo,
                                JobRunner jobRunner) {
        this.repo = repo;
        this.jarFileRepo = jarFileRepo;
        this.jobRunner = jobRunner;
    }

    @PostMapping
    public DeferredResult<ResponseEntity<GradingResult>> create(@RequestBody GradingJob job,
                @RequestParam(required = false, defaultValue = "false") boolean waitUntilDone) {
        if (job.hasResult()) {
            throw new IllegalArgumentException();
        }

        job.getConfig().getDependencies().replaceAll(jarFileRepo::deduplicate);
        repo.save(job);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .pathSegment(job.getId()).build().toUri();

        var deferred = new DeferredResult<ResponseEntity<GradingResult>>(MAX_VALUE);
        Runnable setResult = () -> deferred.setResult(created(location).body(job.getResult()));
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
