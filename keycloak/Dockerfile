FROM quay.io/keycloak/keycloak:26.4.7

# Thème de connexion ASCE-LC (voir themes/asce-lc/login) — packagé dans l'image
# pour ne pas dépendre d'un montage de volume externe sur le serveur cible.
COPY themes/asce-lc /opt/keycloak/themes/asce-lc

# kc.db est une option de build (choix du fournisseur de BDD) : elle doit être fixée ici,
# pas seulement passée en variable d'environnement au démarrage du conteneur.
ENV KC_DB=postgres
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
RUN /opt/keycloak/bin/kc.sh build

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--optimized"]
