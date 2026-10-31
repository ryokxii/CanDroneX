#!/usr/bin/env bash
# Démonstration de la Phase 2 : sécurité B2B, UC-02, UC-04 puis UC-06, par la passerelle.
# Prérequis : docker compose up -d --wait. Chaque cas affiche OK ou ÉCHEC.
set -uo pipefail
cd "$(dirname "$0")/.."
ENV_FILE=${ENV_FILE:-.env}
set -a; . "$ENV_FILE"; set +a
compose() { docker compose --env-file "$ENV_FILE" "$@"; }

KC=${KC:-http://localhost:8081}; GW=${GW:-http://localhost:${GATEWAY_PORT:-8080}}
TOKEN_URL="$KC/realms/candronex/protocol/openid-connect/token"
JSON='Content-Type: application/json'
ECHECS=0

jeton() { curl -s -X POST "$TOKEN_URL" -d grant_type=client_credentials -d client_id="$1" -d client_secret="$2" \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])'; }
code() { curl -s -o /tmp/candronex-demo.json -w "%{http_code}" "$@"; }
champ() { python3 -c "import json;print(json.load(open('/tmp/candronex-demo.json'))$1)"; }
verifier() {  # verifier <libellé> <attendu> <obtenu>
  if [[ "$2" == "$3" ]]; then echo "   OK     $1 -> $3"; else echo "   ÉCHEC  $1 -> $3 (attendu $2)"; ECHECS=$((ECHECS + 1)); fi
}
# Jeton dont le contenu est modifié (azp) ; la signature d'origine ne correspond plus.
modifie() { python3 - "$1" <<'PY'
import sys, json, base64
b64 = lambda b: base64.urlsafe_b64encode(b).rstrip(b"=").decode()
h, p, s = sys.argv[1].split(".")
c = json.loads(base64.urlsafe_b64decode(p + "=" * (-len(p) % 4))); c["azp"] = "aerotech-demo"
print(f"{h}.{b64(json.dumps(c).encode())}.{s}")
PY
}
non_signe() { python3 - "$1" <<'PY'
import sys, json, base64
b64 = lambda b: base64.urlsafe_b64encode(b).rstrip(b"=").decode()
print(f"{b64(json.dumps({'alg': 'none', 'typ': 'JWT'}).encode())}.{sys.argv[1].split('.')[1]}.")
PY
}
# IMSI et ICCID neufs à chaque exécution ; l'ICCID porte un chiffre de contrôle de Luhn.
identite() { python3 - <<'PY'
import random
n = f"{random.randrange(10**9):09d}"
base = "893027" + n + "000"
s = sum((d * 2 - 9 if d * 2 > 9 else d * 2) if i % 2 == 0 else d for i, d in enumerate(map(int, reversed(base))))
print(f"30272{n}0", base + str((10 - s % 10) % 10))
PY
}

echo "1) Jetons (client_credentials, realm candronex)"
INS=$(jeton inspectra-demo "$INSPECTRA_DEMO_SECRET"); AER=$(jeton aerotech-demo "$AEROTECH_DEMO_SECRET")
verifier "mauvais secret" "unauthorized_client" "$(curl -s -X POST "$TOKEN_URL" -d grant_type=client_credentials \
  -d client_id=inspectra-demo -d client_secret=faux | python3 -c 'import sys,json;print(json.load(sys.stdin).get("error"))')"

echo "2) Sécurité de la passerelle et des services"
verifier "sans jeton" 401 "$(code $GW/api/v1/drones)"
verifier "jeton invalide" 401 "$(code -H 'Authorization: Bearer abc.def.ghi' $GW/api/v1/drones)"
verifier "jeton modifié" 401 "$(code -H "Authorization: Bearer $(modifie "$INS")" $GW/api/v1/drones)"
verifier "jeton non signé" 401 "$(code -H "Authorization: Bearer $(non_signe "$INS")" $GW/api/v1/drones)"
verifier "route interne par la passerelle" 404 "$(code -H "Authorization: Bearer $INS" "$GW/internal/v1/service-activations?orderLineId=x")"

echo "3) UC-02 — enregistrer un drone"
# Corps JSON préparés d'avance : bash 3.2 (macOS) déforme un JSON écrit dans "$(...)".
read -r IMSI ICCID <<< "$(identite)"
DRONE_BODY='{"imsi":"'$IMSI'","iccid":"'$ICCID'","simType":"ESIM"}'
MALFORME='{"imsi":"30272012345","iccid":"8999700000000010239","simType":"SIM"}'
verifier "enregistrement" 201 "$(code -X POST $GW/api/v1/drones -H "Authorization: Bearer $INS" -H "$JSON" -d "$DRONE_BODY")"
DRONE=$(champ '["droneId"]')
verifier "IMSI déjà enregistré" 409 "$(code -X POST $GW/api/v1/drones -H "Authorization: Bearer $AER" -H "$JSON" -d "$DRONE_BODY")"
verifier "IMSI mal formé" 422 "$(code -X POST $GW/api/v1/drones -H "Authorization: Bearer $INS" -H "$JSON" -d "$MALFORME")"
verifier "drone d'Inspectra lu par Aerotech" 404 "$(code -H "Authorization: Bearer $AER" $GW/api/v1/drones/$DRONE)"
verifier "même lecture, X-Client-Id forgé" 404 "$(code -H "Authorization: Bearer $AER" -H 'X-Client-Id: inspectra-demo' \
  $GW/api/v1/drones/$DRONE)"

echo "4) UC-04 — commander C2 et Imagerie"
KEY="demo-$(date +%s)"
COMMANDE='{"orderLines":[{"droneId":"'$DRONE'","serviceSpecificationId":"C2_URLLC"},{"droneId":"'$DRONE'","serviceSpecificationId":"IMAGERY_EMBB"}]}'
AUTRE='{"orderLines":[{"droneId":"'$DRONE'","serviceSpecificationId":"C2_URLLC"}]}'
verifier "commande" 201 "$(code -X POST $GW/api/v1/service-orders -H "Authorization: Bearer $INS" -H "$JSON" \
  -H "Idempotency-Key: $KEY" -d "$COMMANDE")"
ORDER=$(champ '["orderId"]'); echo "          orderId=$ORDER, état $(champ '["status"]')"
verifier "rejeu, même contenu" 200 "$(code -X POST $GW/api/v1/service-orders -H "Authorization: Bearer $INS" -H "$JSON" \
  -H "Idempotency-Key: $KEY" -d "$COMMANDE")"
verifier "rejeu, même commande" "$ORDER" "$(champ '["orderId"]')"
verifier "même clé, autre contenu" 409 "$(code -X POST $GW/api/v1/service-orders -H "Authorization: Bearer $INS" -H "$JSON" \
  -H "Idempotency-Key: $KEY" -d "$AUTRE")"
verifier "sans Idempotency-Key" 400 "$(code -X POST $GW/api/v1/service-orders -H "Authorization: Bearer $INS" -H "$JSON" \
  -d "$COMMANDE")"

echo "5) UC-06 — activation par le réseau simulé (parcours dans les journaux)"
sleep "${ATTENTE:-6}"
[[ -n "$ORDER" ]] && compose logs --no-color order-service | grep "orderId=$ORDER" \
  | sed -E 's/correlationId=[^ ]+ //' | cut -c1-190
ETAT=$(compose logs --no-color order-service | grep "event=order-status orderId=$ORDER" | tail -1 | sed -E 's/.*state=([A-Z_]+).*/\1/')
verifier "commande terminée (COMPLETED ou PARTIAL)" ok "$([[ $ETAT == COMPLETED || $ETAT == PARTIAL ]] && echo ok || echo "$ETAT")"

echo; [[ $ECHECS -eq 0 ]] && echo "Tous les cas sont conformes." || { echo "$ECHECS cas non conformes."; exit 1; }
