# docker build -t ceres-in-docker:0.5 .
# this file is for local development use only
FROM adoptopenjdk/openjdk14:alpine-jre

ARG ARTIFACT_VERSION

COPY target/ceres-$ARTIFACT_VERSION.jar ceres-in-docker.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "ceres-in-docker.jar"]
