#!/usr/bin/env bash

if [ ! -f dump-clean.sql ]; then
  cat <dump.sql |
      tr -d '\r' |                                      # remove \r
      sed 's/ *$//' |                                   # remove trailing spaces
      sed -zE 's/([^;])\n */\1 /g' |                    # remove newlines in the middle of SQL statements
      sed -E 's/^([A-Z ]*)"PUBLIC"\."([^"]+)"/\1\2/' |  # remove schema prefix and quotes
      cat >dump-clean.sql
fi

docker build -t psql .

cat <dump-clean.sql | grep 'INSERT INTO COURSE ' | docker run -i --network grading-server_default psql
