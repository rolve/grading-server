package ch.trick17.gradingserver.webapp.controller;

import ch.trick17.gradingserver.webapp.model.Submission;
import ch.trick17.gradingserver.webapp.model.SubmissionRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
@RequestMapping("/courses/{courseId}/problem-sets/{probId}/solutions/{solId}/submissions")
public class SubmissionController {

    // TODO: grant access only to the authors of a submission

    private final SubmissionRepository repo;

    public SubmissionController(SubmissionRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/{id}/")
    public String submissionPage(@PathVariable int courseId, @PathVariable int probId,
                                 @PathVariable int solId, @PathVariable int id,
                                 Model model) {
        var submission = findSubmission(courseId, probId, solId, id);
        model.addAttribute("submission", submission);
        return "submissions/submission";
    }

    private Submission findSubmission(int courseId, int probId, int solId, int id) {
        var submission = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
        if (submission.getSolution().getId() != solId ||
                submission.getSolution().getProblemSet().getId() != probId ||
                submission.getSolution().getProblemSet().getCourse().getId() != courseId) {
            // silly, but allowing any course/problem set ID would be too
            throw new ResponseStatusException(NOT_FOUND);
        }
        return submission;
    }
}