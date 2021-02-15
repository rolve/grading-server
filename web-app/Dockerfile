FROM openjdk:15-jdk-alpine
WORKDIR app
RUN addgroup -S spring && adduser -S spring -G spring
RUN chown spring:spring .
USER spring:spring
COPY target/dependency/*.jar lib/
COPY target/*.jar app.jar
ENTRYPOINT ["java","-cp", "app.jar:lib/*", "--enable-preview", "ch.trick17.gradingserver.webapp.GradingServerWebApp"]
