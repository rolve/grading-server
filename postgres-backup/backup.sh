#!/usr/bin/env sh

cd /backup || exit
pg_dump | gzip > "backup-$(date +%Y-%m-%d-%H-%M-%S).sql.gz"

# Keep the last 7 backups
ls -1tr | head -n -7 | xargs rm -f --
