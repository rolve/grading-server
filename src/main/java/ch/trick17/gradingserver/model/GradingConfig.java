package ch.trick17.gradingserver.model;

import javax.persistence.Embeddable;
import javax.persistence.Lob;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

@Embeddable
public class GradingConfig {

    @Lob
    private String testClass;
    private GradingOptions options;

    protected GradingConfig() {}

    public GradingConfig(String testClass, GradingOptions options) {
        this.testClass = requireNonNull(testClass);
        this.options = requireNonNull(options);
    }

    public String getTestClass() {
        return testClass;
    }

    public GradingOptions getOptions() {
        return options;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (GradingConfig) obj;
        return testClass.equals(that.testClass) && options.equals(that.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(options, testClass);
    }
}
