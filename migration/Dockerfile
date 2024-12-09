FROM alpine:edge
RUN apk --no-cache add postgresql17-client
ENV PGHOST=postgres
ENV PGUSER=grading-server
ENV PGPASSWORD=secret
ENTRYPOINT ["/usr/bin/psql"]
