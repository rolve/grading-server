package ch.trick17.gradingserver;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

@Embeddable
public class GradingConfig {

    @Column
    private GradingOptions options;
    @Column
    private String testClass;

    protected GradingConfig() {}

    public GradingConfig(GradingOptions options, String testClass) {
        this.options = requireNonNull(options);
        this.testClass = requireNonNull(testClass);
    }

    public GradingOptions options() {
        return options;
    }

    public String testClass() {
        return testClass;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (GradingConfig) obj;
        return Objects.equals(this.options, that.options) &&
                Objects.equals(this.testClass, that.testClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(options, testClass);
    }

    @Override
    public String toString() {
        return "GradingConfig[" +
                "options=" + options + ", " +
                "testClass=" + testClass + ']';
    }
}
