FROM grafana/grafana:11.4.0
COPY grafana.ini /etc/grafana/grafana.ini
ENV GF_PATHS_PROVISIONING=/etc/grafana/provisioning
COPY datasources /etc/grafana/provisioning/datasources
