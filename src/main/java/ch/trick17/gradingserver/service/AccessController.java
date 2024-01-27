package ch.trick17.gradingserver.service;

import ch.trick17.gradingserver.model.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import static java.util.Locale.ROOT;

@Service("access")
public class AccessController {

    private final CourseRepository courseRepo;

    public AccessController(CourseRepository courseRepo) {
        this.courseRepo = courseRepo;
    }

    @Transactional
    public boolean check(Course course) {
        if (currentPrincipal() instanceof User user) {
            return user.getRoles().contains(Role.ADMIN) ||
                   course.getLecturers().contains(user);
        } else {
            return false;
        }
    }

    @Transactional
    public boolean check(int courseId) {
        return courseRepo.findById(courseId).map(this::check).orElse(false);
    }

    @Transactional
    public boolean check(ProblemSet problemSet) {
        return check(problemSet.getCourse());
    }

    @Transactional
    public boolean check(Submission submission) {
        return check(submission.getSolution().getProblemSet());
    }

    @Transactional
    public boolean hasRole(String roleString) {
        var role = Role.valueOf(roleString.toUpperCase(ROOT));
        if (currentPrincipal() instanceof User user) {
            return user.getRoles().stream().anyMatch(r -> r.includes(role));
        } else {
            return false;
        }
    }

    private static Object currentPrincipal() {
        return SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}