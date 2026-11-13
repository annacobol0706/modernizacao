# ---- build do WAR (Java 8 + Maven 3) ----
FROM maven:3.8.8-eclipse-temurin-8 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B package

# ---- runtime (Tomcat 8.5 + JDK 8) ----
FROM tomcat:8.5-jdk8-temurin
RUN rm -rf /usr/local/tomcat/webapps/* \
    && mkdir -p /usr/local/tomcat/liquidacao
COPY --from=build /build/target/anycompany.war /usr/local/tomcat/webapps/anycompany.war
EXPOSE 8080
CMD ["catalina.sh", "run"]
