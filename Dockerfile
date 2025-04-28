FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

WORKDIR /app

COPY pom.xml ./
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk-alpine AS jre
RUN $JAVA_HOME/bin/jlink \
    --add-modules ALL-MODULE-PATH \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=2 \
    --output /jre-minimal

FROM alpine:3.19

COPY --from=jre /jre-minimal /opt/jre

COPY --from=build /app/target/*.jar /app/app.jar

RUN apk add --no-cache tzdata && \
    ln -sf /usr/share/zoneinfo/Europe/Moscow /etc/localtime

ENV JAVA_HOME=/opt/jre
ENV PATH="$JAVA_HOME/bin:$PATH"
ENV JAVA_OPTS="-XX:MaxRAM=128M -XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Xss512k -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]