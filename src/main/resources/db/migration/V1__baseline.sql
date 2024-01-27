create sequence HIBERNATE_SEQUENCE;

create table AUTHOR
(
    ID   INTEGER not null
        primary key,
    NAME VARCHAR(255)
        constraint UK_OR6K6JMYWERXBME223C988BMG
            unique
);

create table COURSE
(
    ID        INTEGER not null
        primary key,
    NAME      VARCHAR(255),
    QUALIFIER VARCHAR(255),
    KIND      VARCHAR(255),
    YEAR      INTEGER not null
);

create table PROBLEM_SET
(
    ID                    INTEGER not null
        primary key,
    ANONYMOUS             BOOLEAN not null,
    DEADLINE              TIMESTAMP,
    COMPILER              INTEGER,
    PERM_RESTRICTIONS     BOOLEAN not null,
    REP_TIMEOUT_MILLIS    BIGINT  not null,
    REPETITIONS           INTEGER not null,
    TEST_TIMEOUT_MILLIS   BIGINT  not null,
    PROJECT_ROOT          VARCHAR(255),
    STRUCTURE             INTEGER,
    TEST_CLASS            CLOB,
    HIDDEN                BOOLEAN not null,
    NAME                  VARCHAR(255),
    REGISTERING_SOLUTIONS BOOLEAN not null,
    COURSE_ID             INTEGER,
    constraint FKHJA1FO3KD9HEP5P2TDJ4BCTA6
        foreign key (COURSE_ID) references COURSE
);

create table USER
(
    ID           INTEGER not null
        primary key,
    DISPLAY_NAME VARCHAR(255),
    PASSWORD     VARCHAR(255),
    USERNAME     VARCHAR(255)
        constraint UK_SB8BBOUER5WAK8VYIIY4PF2BX
            unique
);

create table ACCESS_TOKEN
(
    ID       INTEGER not null
        primary key,
    HOST     VARCHAR(255),
    TOKEN    VARCHAR(255),
    OWNER_ID INTEGER,
    constraint FKKBQ86BAT8UUS42FW8PSMGECGD
        foreign key (OWNER_ID) references USER
);

create table COURSE_LECTURERS
(
    COURSE_ID    INTEGER not null,
    LECTURERS_ID INTEGER not null,
    primary key (COURSE_ID, LECTURERS_ID),
    constraint FK8L2VYR4LKTHGEUTSEKC3ICKO7
        foreign key (COURSE_ID) references COURSE,
    constraint FKEQHCNSUVQSKR19CW60P94DNNW
        foreign key (LECTURERS_ID) references USER
);

create table SOLUTION
(
    ID              INTEGER not null
        primary key,
    IGNORED_PUSHERS CLOB,
    REPO_URL        VARCHAR(255),
    ACCESS_TOKEN_ID INTEGER,
    PROBLEM_SET_ID  INTEGER,
    constraint FKD2APC1UJH8V9T7ON93NLSS0W1
        foreign key (PROBLEM_SET_ID) references PROBLEM_SET,
    constraint FKMQ6GASK9LGJUR09CVPE3V7SQY
        foreign key (ACCESS_TOKEN_ID) references ACCESS_TOKEN
);

create table SOLUTION_AUTHORS
(
    SOLUTION_ID INTEGER not null,
    AUTHORS_ID  INTEGER not null,
    primary key (SOLUTION_ID, AUTHORS_ID),
    constraint FK8IP6MQ7VI1LXF40FSXLUYMEDU
        foreign key (AUTHORS_ID) references AUTHOR,
    constraint FKLQRWU81G5U02GOU55FD761PMW
        foreign key (SOLUTION_ID) references SOLUTION
);

create table SUBMISSION
(
    ID              INTEGER not null
        primary key,
    COMMIT_HASH     VARCHAR(255),
    GRADING_STARTED BOOLEAN not null,
    RECEIVED_DATE   TIMESTAMP,
    DETAILS         CLOB,
    ERROR           CLOB,
    FAILED_TESTS    CLOB,
    PASSED_TESTS    CLOB,
    PROPERTIES      VARCHAR(255),
    SOLUTION_ID     INTEGER,
    constraint FK8Q2SHNVT2H73Y8ESDC86KOHRQ
        foreign key (SOLUTION_ID) references SOLUTION
);

create table USER_ROLES
(
    USER_ID INTEGER not null,
    ROLES   INTEGER,
    constraint FK55ITPPKW3I07DO3H7QOCLQD4K
        foreign key (USER_ID) references USER
);
