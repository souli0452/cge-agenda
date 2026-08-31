# Build : compile le jar avec Maven dans un conteneur jetable
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# -Dhttp.keepAlive=false / pool=false : contourne un bug connu de réutilisation de
# connexion HTTP qui tronque aléatoirement des téléchargements ("Premature end of
# Content-Length") lors des builds Maven en conteneur.
ENV MAVEN_OPTS="-Dhttp.keepAlive=false -Dmaven.wagon.http.pool=false -Dmaven.wagon.httpconnectionManager.ttlSeconds=120"
COPY pom.xml .
# Retry en cas de coupure réseau transitoire pendant le téléchargement des dépendances
# (constaté en environnement de build instable : "Premature end of Content-Length").
RUN for i in 1 2 3; do mvn -B dependency:go-offline && exit 0 || sleep 10; done; exit 1
COPY src ./src
RUN for i in 1 2 3; do mvn -B -DskipTests package && exit 0 || sleep 10; done; exit 1

# Run : JRE + client PostgreSQL 18 (requis par le module de sauvegarde : pg_dump/pg_restore
# doivent correspondre à la version majeure du serveur). Base Ubuntu pour pouvoir ajouter
# le dépôt APT officiel PGDG — Alpine ne fournit pas de paquet postgresql18-client fiable.
FROM eclipse-temurin:21-jre-jammy
RUN i=0; until apt-get update && apt-get install -y --no-install-recommends curl ca-certificates gnupg \
        && install -d /usr/share/postgresql-common/pgdg \
        && curl -o /usr/share/postgresql-common/pgdg/apt.postgresql.org.asc --fail https://www.postgresql.org/media/keys/ACCC4CF8.asc \
        && echo "deb [signed-by=/usr/share/postgresql-common/pgdg/apt.postgresql.org.asc] https://apt.postgresql.org/pub/repos/apt jammy-pgdg main" > /etc/apt/sources.list.d/pgdg.list \
        && apt-get update && apt-get install -y --no-install-recommends postgresql-client-18; \
    do i=$((i+1)); if [ $i -ge 3 ]; then exit 1; fi; sleep 10; done \
    && apt-get purge -y curl gnupg && apt-get autoremove -y \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /build/target/cge-agenda-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
