#!/usr/bin/env bash

docker build -t psql .

cat test.sql | grep -e "^SELECT " | xargs -d '\n' docker run --network grading-server_default psql -c
