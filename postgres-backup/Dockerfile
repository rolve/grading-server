FROM alpine:edge
# TODO: change to release version once postgresql17-client is available
RUN apk --no-cache add postgresql17-client
COPY backup.sh /etc/periodic/daily/backup.sh
CMD ["/usr/sbin/crond", "-f"]
