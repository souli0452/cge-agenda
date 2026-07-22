#!/bin/bash
# Exécuté automatiquement par l'image postgres au tout premier démarrage
# (volume vide) : la base applicative est déjà créée via POSTGRES_DB,
# celle-ci ajoute la base dédiée à Keycloak.
set -e
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE keycloak;
EOSQL
