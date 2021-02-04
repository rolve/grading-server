package ch.trick17.gradingserver.webapp.model;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

@Embeddable
public class Term {

    @Column
    private int year;
    @Column
    private String kind; // e.g. FS or HS

    protected Term() {}

    public Term(int year, String kind) {
        this.year = year;
        this.kind = kind;
    }

    public int getYear() {
        return year;
    }

    public String getKind() {
        return kind;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Term)) return false;
        Term term = (Term) o;
        return year == term.year && Objects.equals(kind, term.kind);
    }

    @Override
    public int hashCode() {
        return Objects.hash(year, kind);
    }
}
