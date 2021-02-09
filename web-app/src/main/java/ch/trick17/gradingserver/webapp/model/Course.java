package ch.trick17.gradingserver.webapp.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static javax.persistence.CascadeType.ALL;

@Entity
public class Course {

    @Id
    @GeneratedValue
    private int id;
    private String name;
    private Term term;
    private String qualifier;
    @OneToMany(mappedBy = "course", cascade = ALL, orphanRemoval = true)
    private List<ProblemSet> problemSets = new ArrayList<>();

    protected Course() {}

    public Course(String name, Term term, String qualifier) {
        this.name = requireNonNull(name);
        this.term = requireNonNull(term);
        this.qualifier = qualifier;
    }

    public int getId() {
        return id;
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

    public List<ProblemSet> getProblemSets() {
        return problemSets;
    }

    public String fullName() {
        return name + " (" + term.getKind() + " " + term.getYear() + " " + qualifier + ")";
    }
}
