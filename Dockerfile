FROM ubuntu:latest
LABEL authors="siakour.drabo"
# Étape 1 : Utiliser une image de base OpenJDK officielle depuis Docker Hub
 FROM openjdk:17-jdk-alpine

# Étape 2 : Définir le répertoire de travail dans le conteneur
 WORKDIR /app

# Étape 3 : Copier le fichier JAR Spring Boot dans le conteneur
 COPY target/my-spring-boot-app.jar /app/my-spring-boot-app.jar

# Étape 4 : Exposer le port sur lequel votre application s'exécute
 EXPOSE 8080

# Étape 5 : Définir la commande pour exécuter votre application Spring Boot
 CMD [ "java" , "-jar" , "/app/my-spring-boot-app.jar" ]

ENTRYPOINT ["top", "-b"]