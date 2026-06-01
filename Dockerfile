FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S dockerUser && adduser -S dockerUser -G dockerUser
USER dockerUser:dockerUser

VOLUME /tmp

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java","-jar","/app.jar"]