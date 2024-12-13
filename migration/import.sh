#!/usr/bin/env bash

# expects a dump.sql file in the same directory, created using the following H2 command:
#   SCRIPT BLOCKSIZE 2147483647 TO 'dump.sql';


# convert dump to Postgres format

if [ ! -f dump-converted.sql ]; then
  cat <dump.sql |
      sed 's/ *$//' |                                     # remove trailing spaces
      sed -zE 's/([^;])\n */\1 /g' |                      # remove newlines in the middle of SQL statements
      sed -E 's/^([A-Z ]*)"PUBLIC"\."([^"]+)"/\1\2/' |    # remove schema prefix and quotes around table names
      sed -E "s/ STRINGDECODE\('([^']+)'\), / '\1', /g" | # remove STRINGDECODE(...)
      sed 's/\\u00c4/Ä/g' |                               # unescape umlauts
      sed 's/\\u00e4/ä/g' |
      sed 's/\\u00d6/Ö/g' |
      sed 's/\\u00f6/ö/g' |
      sed 's/\\u00dc/Ü/g' |
      sed 's/\\u00fc/ü/g' |
      sed 's/\\u00e7/ç/g' |
      sed "s/, X'/, '\\\\x/g" |                           # replace X'...' with '\x...'
      cat >dump-converted.sql
fi


# build Docker image with Postgres client

docker build -t psql .
shopt -s expand_aliases
alias psql='docker run -i --network grading-server_default psql'


# remove existing data

echo 'DELETE FROM submission;' | psql
echo 'DELETE FROM solution_authors;' | psql
echo 'DELETE FROM author;' | psql
echo 'DELETE FROM solution;' | psql
echo 'DELETE FROM access_token;' | psql
echo 'DELETE FROM problem_set_dependencies;' | psql
echo 'DELETE FROM jar_file;' | psql
echo 'DELETE FROM problem_set;' | psql
echo 'DELETE FROM course_lecturers;' | psql
echo 'DELETE FROM course;' | psql
echo 'DELETE FROM user_roles;' | psql
echo 'DELETE FROM "user";' | psql


# import table data

grep <dump-converted.sql 'INSERT INTO USER ' |
    sed 's/INSERT INTO USER /INSERT INTO "user" /' |        # quotes are necessary here
    psql

grep <dump-converted.sql 'INSERT INTO USER_ROLES ' |
    sed -E "s/\(([0-9]+), 0\)/(\1, 'LECTURER')/g" |         # use enum values for role
    sed -E "s/\(([0-9]+), 1\)/(\1, 'ADMIN')/g" |
    psql

grep <dump-converted.sql 'INSERT INTO COURSE ' | psql

grep <dump-converted.sql 'INSERT INTO COURSE_LECTURERS ' | psql

grep <dump-converted.sql 'INSERT INTO PROBLEM_SET ' |
    sed "s/', 0, /', 'ECLIPSE', /g" |                       # use enum values for project_structure
    sed "s/', 1, /', 'MAVEN', /g" |
    sed "s/, 0, JSON/, 'WITH_FULL_NAMES', JSON/g" |         # and for display_setting
    sed "s/, 1, JSON/, 'WITH_SHORTENED_NAMES', JSON/g" |
    sed "s/, 2, JSON/, 'ANONYMOUS', JSON/g" |
    sed "s/, 3, JSON/, 'HIDDEN', JSON/g" |
    psql

grep <dump-converted.sql 'INSERT INTO JAR_FILE ' | psql

grep <dump-converted.sql 'INSERT INTO PROBLEM_SET_DEPENDENCIES ' | psql

grep <dump-converted.sql 'INSERT INTO ACCESS_TOKEN ' | psql

grep <dump-converted.sql 'INSERT INTO SOLUTION ' | psql

grep <dump-converted.sql 'INSERT INTO AUTHOR ' | psql

grep <dump-converted.sql 'INSERT INTO SOLUTION_AUTHORS ' | psql

grep <dump-converted.sql 'INSERT INTO SUBMISSION ' | psql


# restart sequence

grep <dump-converted.sql 'CREATE SEQUENCE ' |
    sed 's/CREATE/ALTER/' |
    sed 's/START/RESTART/' |
    psql
