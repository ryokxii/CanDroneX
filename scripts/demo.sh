#!/usr/bin/env bash
# Démonstration de bout en bout, contre l'environnement démarré par docker compose up :
# jeton OAuth 2.0, UC-02, UC-04, rejeu idempotent et principaux refus.
# Rejouable : chaque exécution enregistre un nouveau drone (IMSI et ICCID d'essai aléatoires).
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
# Client de démonstration amorcé par les migrations V4 et V5 — jamais un secret réel.
CLIENT_ID="${CLIENT_ID:-0b6f1c2e-8f4a-4d3b-9c71-5e2a7d9f3b10}"
CLIENT_SECRET="${CLIENT_SECRET:-inspectra-demo-secret}"

BODY="$(mktemp)"
trap 'rm -f "$BODY"' EXIT

# Complète un numéro par son chiffre de contrôle de Luhn (ICCID, UIT-T E.118).
luhn_complete() {
  local body=$1 sum=0 double=1 digit i
  for (( i = ${#body} - 1; i >= 0; i-- )); do
    digit=${body:i:1}
    if (( double )); then
      digit=$(( digit * 2 ))
      (( digit > 9 )) && digit=$(( digit - 9 ))
    fi
    sum=$(( sum + digit ))
    double=$(( 1 - double ))
  done
  echo "${body}$(( (10 - sum % 10) % 10 ))"
}

# PLMN d'essai 999-70 : aucun opérateur réel.
SERIAL=$(( RANDOM * 32768 + RANDOM ))
IMSI="99970$(printf '%010d' "$SERIAL")"
ICCID="$(luhn_complete "8999700$(printf '%011d' "$SERIAL")")"

# Extrait la valeur texte d'un champ JSON de premier niveau.
field() {
  sed -n "s/.*\"$1\":\"\([^\"]*\)\".*/\1/p" "$BODY"
}

# call MÉTHODE CHEMIN CODE_ATTENDU [options curl...] — affiche le code et s'arrête s'il diffère.
call() {
  local method=$1 path=$2 expected=$3
  shift 3
  local status
  status=$(curl -s -o "$BODY" -w '%{http_code}' -X "$method" "$BASE_URL$path" "$@")
  if [[ "$status" != "$expected" ]]; then
    echo "  ✗ $method $path → $status (attendu $expected)"
    cat "$BODY"; echo
    exit 1
  fi
  echo "  ✓ $method $path → $status"
}

echo "1. L'application répond"
call GET /actuator/health 200

echo "2. Jeton d'accès — POST /oauth2/token, flux client_credentials"
call POST /oauth2/token 200 \
  -u "$CLIENT_ID:$CLIENT_SECRET" \
  -d grant_type=client_credentials \
  --data-urlencode 'scope=drones:read drones:write orders:write'
TOKEN="$(field access_token)"
AUTH=(-H "Authorization: Bearer $TOKEN")
JSON=(-H "Content-Type: application/json")

echo "3. UC-02 — enregistrer un drone"
call POST /api/v1/drones 201 "${AUTH[@]}" "${JSON[@]}" \
  -d "{\"imsi\":\"$IMSI\",\"iccid\":\"$ICCID\",\"simType\":\"ESIM\"}"
DRONE_ID="$(field droneId)"
echo "     droneId = $DRONE_ID — la réponse ne contient ni l'IMSI ni l'ICCID"
call GET "/api/v1/drones/$DRONE_ID" 200 "${AUTH[@]}"

echo "4. UC-04 — commander C2 et Imagerie pour ce drone"
KEY="$(uuidgen 2>/dev/null || cat /proc/sys/kernel/random/uuid)"
ORDER="{\"orderLines\":[{\"droneId\":\"$DRONE_ID\",\"serviceSpecificationId\":\"C2_URLLC\"},{\"droneId\":\"$DRONE_ID\",\"serviceSpecificationId\":\"IMAGERY_EMBB\"}]}"
call POST /api/v1/service-orders 201 "${AUTH[@]}" "${JSON[@]}" -H "Idempotency-Key: $KEY" -d "$ORDER"
ORDER_ID="$(field orderId)"
echo "     orderId = $ORDER_ID — caractéristiques figées :"
grep -o '"name":"[a-zA-Z]*","value":"[^"]*"' "$BODY" | sed 's/"name":"\(.*\)","value":"\(.*\)"/       \1 = \2/'

echo "5. Rejeu — même clé, même contenu : 200 et la même commande"
call POST /api/v1/service-orders 200 "${AUTH[@]}" "${JSON[@]}" -H "Idempotency-Key: $KEY" -d "$ORDER"
[[ "$(field orderId)" == "$ORDER_ID" ]] && echo "     même orderId : aucune commande créée"

echo "6. Refus attendus"
call POST /api/v1/service-orders 409 "${AUTH[@]}" "${JSON[@]}" -H "Idempotency-Key: $KEY" \
  -d "{\"orderLines\":[{\"droneId\":\"$DRONE_ID\",\"serviceSpecificationId\":\"C2_URLLC\"}]}"
echo "     même clé, autre contenu → conflit"
call POST /api/v1/drones 409 "${AUTH[@]}" "${JSON[@]}" \
  -d "{\"imsi\":\"$IMSI\",\"iccid\":\"$(luhn_complete 899970099999999999)\",\"simType\":\"SIM\"}"
echo "     IMSI déjà enregistré → conflit"
call POST /api/v1/drones 422 "${AUTH[@]}" "${JSON[@]}" \
  -d "{\"imsi\":\"12345\",\"iccid\":\"$ICCID\",\"simType\":\"SIM\"}"
echo "     IMSI mal formé → règle du domaine"
call POST /api/v1/service-orders 422 "${AUTH[@]}" "${JSON[@]}" -H "Idempotency-Key: $(uuidgen 2>/dev/null || echo autre-cle)" \
  -d "{\"orderLines\":[{\"droneId\":\"$DRONE_ID\",\"serviceSpecificationId\":\"VOICE_5G\"}]}"
echo "     service hors catalogue → règle du domaine"
call GET /api/v1/drones 401
echo "     sans jeton → non authentifié"

echo "Démonstration terminée."
