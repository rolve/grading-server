CREATE TABLE jar_file
(
    id       INT NOT NULL,
    filename VARCHAR(255),
    content  BLOB,
    hash     BINARY(32),
    CONSTRAINT pk_jarfile PRIMARY KEY (id)
);

CREATE TABLE problem_set_dependencies
(
    problem_set_id  INT NOT NULL,
    dependencies_id INT NOT NULL
);

ALTER TABLE jar_file
    ADD CONSTRAINT uc_jarfile_hash UNIQUE (hash);

ALTER TABLE problem_set_dependencies
    ADD CONSTRAINT fk_prosetdep_on_jar_file FOREIGN KEY (dependencies_id) REFERENCES jar_file (id);

ALTER TABLE problem_set_dependencies
    ADD CONSTRAINT fk_prosetdep_on_problem_set FOREIGN KEY (problem_set_id) REFERENCES problem_set (id);