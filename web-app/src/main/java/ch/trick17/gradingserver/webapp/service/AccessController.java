package ch.trick17.gradingserver.webapp.service;

import ch.trick17.gradingserver.webapp.model.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import static ch.trick17.gradingserver.webapp.model.Role.ADMIN;

@Service("access")
public class AccessController {

    private final CourseRepository courseRepo;

    public AccessController(CourseRepository courseRepo) {
        this.courseRepo = courseRepo;
    }

    @Transactional
    public boolean check(Course course) {
        var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User user) {
            return user.getRoles().contains(ADMIN) ||
                    course.getLecturers().contains(user);
        } else {
            return false;
        }
    }

    @Transactional
    public boolean check(int courseId) {
        return check(courseRepo.findById(courseId).orElseThrow());
    }

    @Transactional
    public boolean check(ProblemSet problemSet) {
        return check(problemSet.getCourse());
    }

    @Transactional
    public boolean check(Submission submission) {
        return check(submission.getSolution().getProblemSet());
    }
}
