package ch.trick17.gradingserver.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Comparator;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS;
import static java.lang.Boolean.compare;

@JsonTypeInfo(use = CLASS, property = "@class")
public sealed interface GradingResult
        permits ErrorResult, ImplGradingResult, TestSuiteGradingResult {

    default boolean successful() {
        return !(this instanceof ErrorResult);
    }

    List<String> properties();

    static Comparator<GradingResult> betterFirst() {
        return (r1, r2) -> {
            if (!r1.successful() || !r2.successful()) {
                return compare(r2.successful(), r1.successful());
            } else if (r1 instanceof ImplGradingResult i1 && r2 instanceof ImplGradingResult i2) {
                return i2.compareTo(i1);
            } else if (r1 instanceof TestSuiteGradingResult t1 && r2 instanceof TestSuiteGradingResult t2) {
                return t2.compareTo(t1);
            } else {
                throw new AssertionError("incomparable grading results: " + r1 + ", " + r2);
            }
        };
    }
}
