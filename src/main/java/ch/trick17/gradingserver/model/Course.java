package ch.trick17.gradingserver.model;

import javax.persistence.*;
import java.util.*;

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
    @ManyToMany
    private Set<User> lecturers = new HashSet<>();
    private boolean hidden;

    @OneToMany(mappedBy = "course", cascade = ALL, orphanRemoval = true)
    private List<ProblemSet> problemSets = new ArrayList<>();

    public Course() {}

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

    public Set<User> getLecturers() {
        return lecturers;
    }

    public void setLecturers(Collection<? extends User> lecturers) {
        this.lecturers.clear();
        this.lecturers.addAll(lecturers);
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public List<ProblemSet> getProblemSets() {
        return problemSets;
    }

    public String fullName() {
        var suffix = qualifier != null ? " " + qualifier : "";
        return name + " (" + term.getKind() + " " + term.getYear() + suffix + ")";
    }
}
