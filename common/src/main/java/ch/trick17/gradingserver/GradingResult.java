package ch.trick17.gradingserver;

import ch.trick17.gradingserver.util.StringListConverter;
import com.fasterxml.jackson.annotation.JsonCreator;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import java.util.List;
import java.util.Objects;

import static java.util.List.copyOf;

@Embeddable
public class GradingResult {

    @Column
    private String error;
    @Column
    @Convert(converter = StringListConverter.class)
    private List<String> properties;
    @Column
    @Convert(converter = StringListConverter.class)
    private List<String> passedTests;
    @Column
    @Convert(converter = StringListConverter.class)
    private List<String> failedTests;
    @Column
    private String details;

    protected GradingResult() {}

    @JsonCreator
    public GradingResult(String error, List<String> properties, List<String> passedTests,
                         List<String> failedTests, String details) {
        this.error = error;
        this.properties = properties != null ? copyOf(properties) : null;
        this.passedTests = passedTests != null ? copyOf(passedTests) : null;
        this.failedTests = failedTests != null ? copyOf(failedTests): null;
        this.details = details;
    }

    public String getError() {
        return error;
    }

    public boolean successful() {
        return error != null;
    }

    public List<String> getProperties() {
        return properties;
    }

    public List<String> getPassedTests() {
        return passedTests;
    }

    public List<String> getFailedTests() {
        return failedTests;
    }

    public String getDetails() {
        return details;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (GradingResult) obj;
        return Objects.equals(this.error, that.error) &&
                Objects.equals(this.properties, that.properties) &&
                Objects.equals(this.passedTests, that.passedTests) &&
                Objects.equals(this.failedTests, that.failedTests) &&
                Objects.equals(this.details, that.details);
    }

    @Override
    public int hashCode() {
        return Objects.hash(error, properties, passedTests, failedTests, details);
    }

    @Override
    public String toString() {
        return "GradingResult[" +
                "error=" + error + ", " +
                "properties=" + properties + ", " +
                "passedTests=" + passedTests + ", " +
                "failedTests=" + failedTests + ", " +
                "details=" + details + "]";
    }
}
