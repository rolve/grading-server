package ch.trick17.gradingserver;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.time.Duration;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

@Embeddable
public class GradingOptions {

    @Column
    private Compiler compiler;
    @Column
    private int repetitions;
    @Column
    private long repTimeoutMillis;
    @Column
    private long testTimeoutMillis;
    @Column
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

    public Compiler compiler() {
        return compiler;
    }

    public int repetitions() {
        return repetitions;
    }

    public Duration repTimeout() {
        return Duration.ofMillis(repTimeoutMillis);
    }

    public Duration testTimeout() {
        return Duration.ofMillis(testTimeoutMillis);
    }

    public boolean permRestrictions() {
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
                "repTimeout=" + repTimeout() + ", " +
                "testTimeout=" + testTimeout() + ", " +
                "permRestrictions=" + permRestrictions + ']';
    }


    public enum Compiler {
        JAVAC, ECLIPSE
    }
}
