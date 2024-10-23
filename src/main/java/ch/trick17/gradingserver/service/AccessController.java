package ch.trick17.gradingserver.service;

import ch.trick17.gradingserver.model.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static ch.trick17.gradingserver.model.ProblemSet.DisplaySetting.HIDDEN;
import static ch.trick17.gradingserver.model.Role.ADMIN;
import static java.util.Locale.ROOT;

@Service("access")
public class AccessController {

    private final CourseRepository courseRepo;
    private final ProblemSetRepository problemSetRepo;

    public AccessController(CourseRepository courseRepo,
                            ProblemSetRepository problemSetRepo) {
        this.courseRepo = courseRepo;
        this.problemSetRepo = problemSetRepo;
    }

    @Transactional(readOnly = true)
    public boolean checkReadAccess(Course course) {
        return !course.isHidden() || checkWriteAccess(course);
    }

    @Transactional(readOnly = true)
    public boolean checkWriteAccess(Course course) {
        if (currentPrincipal() instanceof User user) {
            return user.getRoles().contains(ADMIN) ||
                   course.getLecturers().contains(user);
        } else {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean checkReadAccessCourse(int courseId) {
        return courseRepo.findById(courseId).map(this::checkReadAccess).orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean checkWriteAccessCourse(int courseId) {
        return courseRepo.findById(courseId).map(this::checkWriteAccess).orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean checkReadAccess(ProblemSet problemSet) {
        return checkReadAccess(problemSet.getCourse()) &&
               (problemSet.getDisplaySetting() != HIDDEN || checkWriteAccess(problemSet));
    }

    @Transactional(readOnly = true)
    public boolean checkWriteAccess(ProblemSet problemSet) {
        return checkWriteAccess(problemSet.getCourse());
    }

    @Transactional(readOnly = true)
    public boolean checkReadAccessProblemSet(int problemSetId) {
        return problemSetRepo.findById(problemSetId).map(this::checkReadAccess).orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean checkWriteAccess(Submission submission) {
        return checkWriteAccess(submission.getSolution().getProblemSet());
    }

    @Transactional(readOnly = true)
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
