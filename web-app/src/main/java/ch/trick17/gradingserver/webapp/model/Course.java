package ch.trick17.gradingserver.webapp.model;

import javax.persistence.*;

import static java.util.Objects.requireNonNull;

@Entity
public class Course {

    @Id
    @GeneratedValue
    private int id;
    private String name;
    private Term term;
    private String qualifier;

    protected Course() {}

    public Course(String name, Term term, String qualifier) {
        this.name = requireNonNull(name);
        this.term = requireNonNull(term);
        this.qualifier = qualifier;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = requireNonNull(name);
    }

    public Term getTerm() {
        return term;
    }

    public void setTerm(Term term) {
        this.term = requireNonNull(term);
    }

    public String getQualifier() {
        return qualifier;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }
}
