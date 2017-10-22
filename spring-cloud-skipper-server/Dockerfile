FROM openjdk:8u141

ARG JAR_FILE

ADD target/${JAR_FILE} spring-cloud-skipper-server.jar

ENTRYPOINT exec java -Djava.security.egd=file:/dev/./urandom -jar /spring-cloud-skipper-server.jar
