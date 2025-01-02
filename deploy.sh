#!/usr/bin/env bash

# Deploys the app using Docker Compose on a remote server. Assumes that the web-app has already
# been built using `mvn package`. Requires a REMOTE_CONNECTION environment variable with a value
# like "user@host".

DIR_BASE="/tmp/grading-server-"
DIR="$DIR_BASE$(date +%Y-%m-%d-%H-%M-%S)"

# Remove previous dir, then create a new one (the directory cannot be deleted as long as the
# containers are running, due to the secrets that are mounted as volumes).

ssh "$REMOTE_CONNECTION" "
    rm -rf $DIR_BASE* &&
    mkdir -p $DIR"

scp -r \
    .env \
    .secrets \
    compose*.yml \
    Dockerfile \
    grafana \
    nginx \
    postgres-backup \
    target \
    "$REMOTE_CONNECTION:$DIR"

ssh "$REMOTE_CONNECTION" "
    cd $DIR &&
    docker compose -p grading-server up --build --detach"
