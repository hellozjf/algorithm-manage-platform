FROM 192.168.2.150/zrar/openjdk:8-jre-alpine
MAINTAINER hellozjf <908686171@qq.com>

# Add the service itself
ARG JAR_FILE
ADD target/${JAR_FILE}  /app/app.jar

EXPOSE 8080

ENTRYPOINT ["/usr/bin/java", "-agentlib:jdwp=transport=dt_socket,address=5005,server=y,suspend=n", "-jar", "/app/app.jar"]
