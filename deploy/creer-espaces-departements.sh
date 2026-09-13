#!/bin/bash
# Cree les 7 espaces departementaux ASCE-LC sur la VM (agenda.asce-lc.bf).
# A executer TOI-MEME dans un terminal (pas via l'assistant), depuis n'importe
# quelle machine ayant acces a auth.asce-lc.bf et agenda.asce-lc.bf en HTTPS.
#
# Usage : bash creer-espaces-departements.sh
# Le mot de passe est saisi de façon masquee, jamais affiche ni transmis ailleurs.

set -e

AUTH_URL="https://auth.asce-lc.bf/realms/asce-lc-realm/protocol/openid-connect/token"
API_URL="https://agenda.asce-lc.bf/api/v1/cge-agenda/admin/espaces"
CLIENT_ID="agenda-cge"

read -p "Identifiant Keycloak (compte ADMIN) : " KC_USER
read -s -p "Mot de passe : " KC_PASS
echo

TOKEN=$(curl -s -X POST "$AUTH_URL" \
  -d "grant_type=password" \
  -d "client_id=$CLIENT_ID" \
  -d "username=$KC_USER" \
  -d "password=$KC_PASS" \
  -d "scope=openid" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

unset KC_PASS

if [ -z "$TOKEN" ]; then
  echo "Echec de connexion : identifiant/mot de passe incorrect, ou compte sans droit ADMIN."
  exit 1
fi

create() {
  local nom="$1"
  local email="$2"
  local payload
  payload=$(printf '{"nom":"%s","chefEmail":"%s","chefNom":"À affecter"}' "$nom" "$email")
  echo "=== $nom ($email) ==="
  local code
  code=$(curl -s -o /tmp/espace_resp.json -w "%{http_code}" -X POST "$API_URL" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json; charset=utf-8" \
    --data-binary "$payload")
  echo "HTTP $code"
  cat /tmp/espace_resp.json
  echo
  echo
}

create "Département d'Audit et de Contrôle" "audit-controle@asce-lc.bf"
create "Département d'Enquête et d'Investigation" "enquete-investigation@asce-lc.bf"
create "Département des Déclarations d'Intérêt et de Patrimoine" "declarations-patrimoine@asce-lc.bf"
create "Département de la Stratégie Nationale de la Prévention" "strategie-prevention@asce-lc.bf"
create "Département du Suivi des Recommandations et des Actions en Justice" "suivi-recommandations@asce-lc.bf"
create "Secrétariat Général" "secretariat-general@asce-lc.bf"
create "Contrôleur Général d'État et Contrôleur Général d'État Adjoint" "cge-adjoint@asce-lc.bf"

echo "Termine. Verifie sur /admin/espaces que les 7 espaces sont bien crees avant"
echo "de remplacer les emails placeholder par les vraies adresses des chefs."
