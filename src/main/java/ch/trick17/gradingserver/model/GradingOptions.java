package ch.trick17.gradingserver.model;

import javax.persistence.Embeddable;
import java.time.Duration;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

@Embeddable
public class GradingOptions {

    private Compiler compiler;
    private int repetitions;
    private long repTimeoutMillis;
    private long testTimeoutMillis;
    private boolean permRestrictions;

    protected GradingOptions() {}

    public GradingOptions(Compiler compiler, int repetitions, Duration repTimeout,
                          Duration testTimeout, boolean permRestrictions) {
        this.compiler = requireNonNull(compiler);
        this.repetitions = repetitions;
        this.repTimeoutMillis = repTimeout.toMillis();
        this.testTimeoutMillis = testTimeout.toMillis();
        this.permRestrictions = permRestrictions;
    }

    public Compiler getCompiler() {
        return compiler;
    }

    public int getRepetitions() {
        return repetitions;
    }

    public Duration getRepTimeout() {
        return Duration.ofMillis(repTimeoutMillis);
    }

    public Duration getTestTimeout() {
        return Duration.ofMillis(testTimeoutMillis);
    }

    public boolean getPermRestrictions() {
        return permRestrictions;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (GradingOptions) obj;
        return Objects.equals(this.compiler, that.compiler) &&
                this.repetitions == that.repetitions &&
                this.repTimeoutMillis == that.repTimeoutMillis &&
                this.testTimeoutMillis == that.testTimeoutMillis &&
                this.permRestrictions == that.permRestrictions;
    }

    @Override
    public int hashCode() {
        return Objects.hash(compiler, repetitions, repTimeoutMillis, testTimeoutMillis, permRestrictions);
    }

    @Override
    public String toString() {
        return "GradingOptions[" +
                "compiler=" + compiler + ", " +
                "repetitions=" + repetitions + ", " +
                "repTimeout=" + getRepTimeout() + ", " +
                "testTimeout=" + getTestTimeout() + ", " +
                "permRestrictions=" + permRestrictions + ']';
    }

    public enum Compiler {
        JAVAC, ECLIPSE
    }
}
