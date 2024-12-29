package ch.trick17.gradingserver.model;

import ch.trick17.jtt.testrunner.TestMethod;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS;
import static java.util.Comparator.comparingInt;

@JsonTypeInfo(use = CLASS, property = "@class")
public sealed interface GradingResult
        permits ErrorResult, OutdatedResult, ImplGradingResult, TestSuiteGradingResult {

    List<String> properties();
    List<String> detailsFor(String property);

    /**
     * Does not support comparing different types of grading results; these have to be handled
     * before.
     */
    static Comparator<GradingResult> betterFirst() {
        return (r1, r2) -> {
            if (r1 instanceof ImplGradingResult i1 && r2 instanceof ImplGradingResult i2) {
                return i2.compareTo(i1);
            } else if (r1 instanceof TestSuiteGradingResult t1 && r2 instanceof TestSuiteGradingResult t2) {
                return t2.compareTo(t1);
            } else if (r1.getClass() == r2.getClass()) {
                return 0;
            } else {
                throw new AssertionError("incomparable grading results: " + r1 + ", " + r2);
            }
        };
    }

    /**
     * Formats the given test methods as strings, using the simplest possible
     * format that still distinguishes them, based on the given list of all
     * test methods.
     */
    static List<String> formatTestMethods(Collection<TestMethod> tests, List<TestMethod> allTests) {
        var classes = allTests.stream()
                .map(TestMethod::className)
                .distinct()
                .count();
        return tests.stream()
                .sorted(comparingInt(allTests::indexOf))
                .map(m -> classes > 1
                        ? m.className().substring(m.className().lastIndexOf('.') + 1) + "." + m.name()
                        : m.name())
                .toList();
    }
}
