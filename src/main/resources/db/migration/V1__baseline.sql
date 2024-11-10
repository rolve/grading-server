CREATE SEQUENCE hibernate_sequence START WITH 1 INCREMENT BY 1;

CREATE TABLE author
(
    id       INTEGER NOT NULL,
    username VARCHAR(255),
    CONSTRAINT pk_author PRIMARY KEY (id)
);

CREATE TABLE course
(
    id        INTEGER NOT NULL,
    name      VARCHAR(255),
    qualifier VARCHAR(255),
    hidden    BOOLEAN NOT NULL,
    year      INTEGER NOT NULL,
    kind      VARCHAR(255),
    CONSTRAINT pk_course PRIMARY KEY (id)
);

CREATE TYPE display_setting AS ENUM ('WITH_FULL_NAMES', 'WITH_SHORTENED_NAMES', 'ANONYMOUS', 'HIDDEN');

CREATE TYPE project_structure AS ENUM ('ECLIPSE', 'MAVEN');

CREATE TABLE problem_set
(
    id                         INTEGER NOT NULL,
    course_id                  INTEGER,
    name                       VARCHAR(255),
    grading_config             JSONB,
    deadline                   TIMESTAMP WITH TIME ZONE,
    display_setting            display_setting,
    percentage_goal            INTEGER NOT NULL,
    registering_solutions      BOOLEAN NOT NULL,
    project_root               VARCHAR(255),
    structure                  project_structure,
    package_filter             VARCHAR(255),
    CONSTRAINT pk_problemset PRIMARY KEY (id)
);

CREATE TABLE "user"
(
    id           INTEGER NOT NULL,
    username     VARCHAR(255),
    password     VARCHAR(255),
    display_name VARCHAR(255),
    CONSTRAINT pk_user PRIMARY KEY (id)
);

CREATE TABLE access_token
(
    id       INTEGER NOT NULL,
    owner_id INTEGER,
    host     VARCHAR(255),
    token    VARCHAR(255),
    CONSTRAINT pk_accesstoken PRIMARY KEY (id)
);

CREATE TABLE course_lecturers
(
    course_id    INTEGER NOT NULL,
    lecturers_id INTEGER NOT NULL,
    CONSTRAINT pk_course_lecturers PRIMARY KEY (course_id, lecturers_id)
);

CREATE TABLE solution
(
    id                   INTEGER NOT NULL,
    problem_set_id       INTEGER,
    repo_url             VARCHAR(255),
    branch               VARCHAR(255),
    access_token_id      INTEGER,
    ignored_pushers      VARCHAR,
    CONSTRAINT pk_solution PRIMARY KEY (id)
);

CREATE TABLE solution_authors
(
    solution_id INTEGER NOT NULL,
    authors_id  INTEGER NOT NULL,
    CONSTRAINT pk_solution_authors PRIMARY KEY (solution_id, authors_id)
);

CREATE TABLE submission
(
    id              INTEGER NOT NULL,
    solution_id     INTEGER,
    commit_hash     VARCHAR(255),
    received_time   TIMESTAMP WITH TIME ZONE,
    grading_started BOOLEAN NOT NULL,
    result          JSONB,
    CONSTRAINT pk_submission PRIMARY KEY (id)
);

CREATE TYPE role AS ENUM ('LECTURER', 'ADMIN');

CREATE TABLE user_roles
(
    user_id INTEGER NOT NULL,
    roles   role
);

CREATE TABLE jar_file
(
    id       INTEGER NOT NULL,
    filename VARCHAR(255),
    content  BYTEA,
    hash     BYTEA,
    CONSTRAINT pk_jarfile PRIMARY KEY (id)
);

CREATE TABLE problem_set_dependencies
(
    problem_set_id  INTEGER NOT NULL,
    dependencies_id INTEGER NOT NULL
);

ALTER TABLE author
    ADD CONSTRAINT uc_author_username UNIQUE (username);

ALTER TABLE jar_file
    ADD CONSTRAINT uc_jarfile_hash UNIQUE (hash);

ALTER TABLE "user"
    ADD CONSTRAINT uc_user_username UNIQUE (username);

ALTER TABLE access_token
    ADD CONSTRAINT FK_ACCESSTOKEN_ON_OWNER FOREIGN KEY (owner_id) REFERENCES "user" (id);

ALTER TABLE problem_set
    ADD CONSTRAINT FK_PROBLEMSET_ON_COURSE FOREIGN KEY (course_id) REFERENCES course (id);

ALTER TABLE solution
    ADD CONSTRAINT FK_SOLUTION_ON_ACCESSTOKEN FOREIGN KEY (access_token_id) REFERENCES access_token (id);

ALTER TABLE solution
    ADD CONSTRAINT FK_SOLUTION_ON_PROBLEMSET FOREIGN KEY (problem_set_id) REFERENCES problem_set (id);

ALTER TABLE submission
    ADD CONSTRAINT FK_SUBMISSION_ON_SOLUTION FOREIGN KEY (solution_id) REFERENCES solution (id);

ALTER TABLE course_lecturers
    ADD CONSTRAINT fk_coulec_on_course FOREIGN KEY (course_id) REFERENCES course (id);

ALTER TABLE course_lecturers
    ADD CONSTRAINT fk_coulec_on_user FOREIGN KEY (lecturers_id) REFERENCES "user" (id);

ALTER TABLE problem_set_dependencies
    ADD CONSTRAINT fk_prosetdep_on_jar_file FOREIGN KEY (dependencies_id) REFERENCES jar_file (id);

ALTER TABLE problem_set_dependencies
    ADD CONSTRAINT fk_prosetdep_on_problem_set FOREIGN KEY (problem_set_id) REFERENCES problem_set (id);

ALTER TABLE solution_authors
    ADD CONSTRAINT fk_solaut_on_author FOREIGN KEY (authors_id) REFERENCES author (id);

ALTER TABLE solution_authors
    ADD CONSTRAINT fk_solaut_on_solution FOREIGN KEY (solution_id) REFERENCES solution (id);

ALTER TABLE user_roles
    ADD CONSTRAINT fk_user_roles_on_user FOREIGN KEY (user_id) REFERENCES "user" (id);