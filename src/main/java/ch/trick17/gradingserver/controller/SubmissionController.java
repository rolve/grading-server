package ch.trick17.gradingserver.controller;

import ch.trick17.gradingserver.model.Submission;
import ch.trick17.gradingserver.model.SubmissionRepository;
import ch.trick17.gradingserver.model.TestSuiteGradingResult;
import ch.trick17.gradingserver.service.GradingService;
import ch.trick17.gradingserver.service.TestSuiteResultService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Controller
@RequestMapping("/courses/{courseId}/problem-sets/{probId}/solutions/{solId}/submissions")
public class SubmissionController {

    // TODO: grant access only to the authors of a submission

    private final SubmissionRepository repo;
    private final GradingService gradingService;
    private final TestSuiteResultService testSuiteResultService;

    public SubmissionController(SubmissionRepository repo, GradingService gradingService,
                                TestSuiteResultService testSuiteResultService) {
        this.repo = repo;
        this.gradingService = gradingService;
        this.testSuiteResultService = testSuiteResultService;
    }

    @GetMapping("/{id}/")
    @PreAuthorize("this.findSubmission(#courseId, #probId, #solId, #id).solution.problemSet.displaySetting.name != 'HIDDEN' || hasRole('LECTURER')")
    public String submissionPage(@PathVariable int courseId, @PathVariable int probId,
                                 @PathVariable int solId, @PathVariable int id,
                                 Model model) {
        var submission = findSubmission(courseId, probId, solId, id);
        var allSubmissions = submission.getSolution().getSubmissions().stream()
                .sorted(comparing(Submission::getReceivedDate, reverseOrder()))
                .collect(Collectors.toList());
        model.addAttribute("submission", submission);
        if (submission.getResult() instanceof TestSuiteGradingResult result) {
            var suggestions = testSuiteResultService.getSuggestions(submission, result);
            model.addAttribute("suggestions", suggestions);
        }
        model.addAttribute("allSubmissions", allSubmissions);
        return "submissions/submission";
    }

    @PostMapping("/{id}/re-grade")
    public String reGrade(@PathVariable int courseId, @PathVariable int probId,
                          @PathVariable int solId, @PathVariable int id) {
        var submission = findSubmission(courseId, probId, solId, id);
        gradingService.grade(submission); // async
        return "redirect:./";
    }

    public Submission findSubmission(int courseId, int probId, int solId, int id) {
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
