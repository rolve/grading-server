package ch.trick17.gradingserver.view;

import ch.trick17.gradingserver.model.Author;
import ch.trick17.gradingserver.model.ProblemSet;
import ch.trick17.gradingserver.model.Submission;

import java.util.Set;

/**
 * Separate class used to render views, able to show the state of a solution at
 * an earlier point in time.
 */
public record SolutionView(
        int id,
        ProblemSet problemSet,
        Set<Author> authors,
        Submission latestSubmission) {}
