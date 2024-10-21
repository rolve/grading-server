package ch.trick17.gradingserver.service;

import ch.trick17.gradingserver.model.*;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static ch.trick17.gradingserver.model.Role.ADMIN;
import static java.lang.Integer.parseInt;
import static java.util.Locale.ROOT;

@Service("access")
public class AccessController {

    private final CourseRepository courseRepo;

    public AccessController(CourseRepository courseRepo) {
        this.courseRepo = courseRepo;
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
    public boolean checkReadAccess(int courseId) {
        return courseRepo.findById(courseId).map(this::checkReadAccess).orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean checkWriteAccess(int courseId) {
        return courseRepo.findById(courseId).map(this::checkWriteAccess).orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean checkWriteAccess(ProblemSet problemSet) {
        return checkWriteAccess(problemSet.getCourse());
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

    public AuthorizationManager<RequestAuthorizationContext> readAccessChecker() {
        return (authentication, context) -> {
            var courseId = context.getVariables().get("courseId");
            try {
                return new AuthorizationDecision(checkReadAccess(parseInt(courseId)));
            } catch (NumberFormatException e) {
                return new AuthorizationDecision(false);
            }
        };
    }

    public AuthorizationManager<RequestAuthorizationContext> writeAccessChecker() {
        return (authentication, context) -> {
            var courseId = context.getVariables().get("courseId");
            try {
                return new AuthorizationDecision(checkWriteAccess(parseInt(courseId)));
            } catch (NumberFormatException e) {
                return new AuthorizationDecision(false);
            }
        };
    }
}
