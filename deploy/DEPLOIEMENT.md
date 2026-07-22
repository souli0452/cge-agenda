# Déploiement CGE Agenda (VM Windows Server, Docker à côté du WAMP existant)

> Stack testée de bout en bout en local (build des 4 images, base neuve, migration
> Flyway complète, import du realm, thème de connexion, démarrage backend/frontend)
> avant d'écrire ce guide. Les pièges déjà rencontrés et corrigés dans les fichiers
> versionnés : PostgreSQL 18+ exige un volume monté sur `/var/lib/postgresql` (pas
> `.../data`) ; `KC_DB` est une option de *build* Keycloak (fixée dans `keycloak/Dockerfile`,
> pas seulement en variable d'environnement) ; le build du backend utilise
> `network: host` pour contourner un problème de téléchargements Maven tronqués observé
> sur certains hôtes Windows/WSL2.

## Pré-requis sur la VM
- Docker Desktop (ou Docker Engine) installé et démarré.
- DNS : `agenda.asce-lc.bf` et `auth.asce-lc.bf` doivent pointer vers l'IP de cette VM.
- Un certificat SSL valide pour ces deux domaines (wildcard `*.asce-lc.bf` réutilisable,
  ou un certificat par sous-domaine — voir `apache-vhosts.conf`).

## 1. Configurer les secrets
```
cd back/cge-agenda
copy .env.example .env
```
Éditer `.env` et renseigner : `DB_PASSWORD`, `MAIL_PASSWORD`, `KEYCLOAK_ADMIN_PASSWORD`.
Laisser `KEYCLOAK_ADMIN_CLIENT_SECRET=change-me` pour l'instant (étape 3 plus bas).

## 2. Lancer la stack Docker
```
docker compose build
docker compose up -d
docker compose ps
```
Les 4 services (`postgres`, `keycloak`, `backend`, `frontend`) doivent être `running`/`healthy`.
Le realm `asce-lc-realm` (clients, rôles CGE/ADMIN/SECRETAIRE/PROTOCOLE/DIRECTEUR_CABINET,
thème de connexion ASCE-LC) est importé automatiquement au premier démarrage depuis
`deploy/realm-export.json` — aucune recréation manuelle nécessaire.

## 3. Générer le secret du client d'administration
Le secret du client `cge-agenda-admin-service` n'a volontairement pas été inclus dans
l'export (pour ne pas committer un secret réel dans le dépôt). Une fois Keycloak démarré :

1. Ouvrir `http://127.0.0.1:18080` (ou `https://auth.asce-lc.bf` une fois le reverse proxy en place)
2. Se connecter en admin, réaliser `asce-lc-realm` → Clients → `cge-agenda-admin-service` → Credentials
3. Cliquer "Regenerate secret", copier la valeur
4. La coller dans `.env` (`KEYCLOAK_ADMIN_CLIENT_SECRET=...`)
5. `docker compose up -d backend` (redémarre uniquement le backend avec le nouveau secret)

## 4. Configurer Apache/WAMP comme reverse proxy
Voir `apache-vhosts.conf` dans ce dossier : contenu à coller dans le fichier de vhosts
WAMP, modules à activer (proxy, proxy_http, headers, ssl), chemins des certificats à
adapter. Redémarrer les services WampServer après modification.

## 5. Vérification post-déploiement
- `https://auth.asce-lc.bf` → console Keycloak accessible, thème de connexion ASCE-LC visible
- `https://agenda.asce-lc.bf` → application Angular, connexion via Keycloak fonctionnelle
- `https://agenda.asce-lc.bf/api/v1/cge-agenda/...` → répond (proxy API fonctionnel)
- Créer un événement de test, le valider, vérifier la réception d'un e-mail

## Sauvegardes
Le module de sauvegarde intégré (menu Admin > Sauvegardes) utilise `pg_dump`/`pg_restore`
installés dans l'image `backend` (PostgreSQL 18 client, cohérent avec le serveur). Les
fichiers de sauvegarde sont stockés dans le volume Docker `backend_backups`, persistant
entre les redémarrages/mises à jour de conteneur.

## Mettre à jour l'application après un déploiement initial
```
git pull   # dans back/cge-agenda et Font/cge-agenda_front
docker compose build backend frontend
docker compose up -d backend frontend
```
`postgres` et `keycloak` n'ont pas besoin d'être reconstruits pour une mise à jour de code.
