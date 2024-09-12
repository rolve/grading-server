package ch.trick17.gradingserver.model;

import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class Term {

    private int year;
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
    public String toString() {
        return kind + " " + year;
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
