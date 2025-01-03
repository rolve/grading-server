FROM eclipse-temurin:21-alpine
WORKDIR app
RUN mkdir db backup
RUN addgroup -S spring && adduser -S spring -G spring
RUN chown spring:spring . db backup
USER spring:spring
COPY target/dependency/*.jar lib/
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-cp", "app.jar:lib/*", "-Duser.timezone=UTC", "ch.trick17.gradingserver.GradingServer"]
