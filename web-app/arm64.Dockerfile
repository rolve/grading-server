FROM arm64v8/openjdk:15-jdk-slim-buster
WORKDIR app
RUN mkdir db backup
RUN addgroup --system spring && adduser --ingroup spring --system spring
RUN chown spring:spring . db backup
USER spring:spring
COPY target/dependency/*.jar lib/
COPY target/*.jar app.jar
ENTRYPOINT ["java","-cp", "app.jar:lib/*", "--enable-preview", "ch.trick17.gradingserver.webapp.GradingServerWebApp"]
