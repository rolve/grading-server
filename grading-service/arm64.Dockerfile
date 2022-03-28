FROM arm64v8/openjdk:17-jdk-slim-buster
WORKDIR app
RUN addgroup --system spring && adduser --ingroup spring --system spring
RUN chown spring:spring .
USER spring:spring
COPY target/dependency/*.jar lib/
COPY target/*.jar app.jar
ENTRYPOINT ["java","-cp", "app.jar:lib/*", "--enable-preview", "ch.trick17.gradingserver.gradingservice.GradingServiceApplication"]
