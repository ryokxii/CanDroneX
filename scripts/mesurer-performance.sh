#!/usr/bin/env bash
# Mesure du §10.4 : débit par la passerelle (k6) puis P95 du délai d'activation (slice_db).
# Prérequis : docker compose up -d --wait, avec le mock de mesure, par exemple :
#   MOCK_FAILURE_RATE=0.02 MOCK_TIMEOUT_RATE=0.01 docker compose up -d --wait
# Usage : scripts/mesurer-performance.sh [RATE] [RAMP] [STEADY]   (défaut : 600 1m 5m)
set -euo pipefail
cd "$(dirname "$0")/.."
ENV_FILE=${ENV_FILE:-.env}
set -a; . "$ENV_FILE"; set +a
compose() { docker compose --env-file "$ENV_FILE" "$@"; }

RATE=${1:-600}; RAMP=${2:-1m}; STEADY=${3:-5m}
PROJECT=${COMPOSE_PROJECT_NAME:-$(basename "$PWD" | tr '[:upper:]' '[:lower:]')}
NETWORK="${PROJECT}_candronex-net"
RESULTS=load-tests/resultats; mkdir -p "$RESULTS"
STAMP=$(date -u +%Y%m%dT%H%M%SZ)
DEBUT=$(date -u +%Y-%m-%dT%H:%M:%SZ)

echo "== k6 : ${RAMP} de montée, puis ${STEADY} à ${RATE} op/s (début ${DEBUT})"
docker run --rm --network "$NETWORK" -v "$PWD/load-tests:/scripts:ro" -v "$PWD/$RESULTS:/out" \
  -e CLIENT_SECRET="$INSPECTRA_DEMO_SECRET" -e RATE="$RATE" -e RAMP="$RAMP" -e STEADY="$STEADY" \
  grafana/k6:0.54.0 run --summary-export "/out/k6-${STAMP}.json" /scripts/charge-mixte.js \
  | tee "$RESULTS/k6-${STAMP}.txt" || true

echo "== P95 du délai d'activation (services acceptés depuis ${DEBUT})"
compose exec -T slice-db psql -U "$POSTGRES_USER" -d slice -v debut="$DEBUT" -x \
  -f - < load-tests/delai-activation.sql | tee "$RESULTS/p95-${STAMP}.txt"

echo "== File d'activation (journaux de slice-service)"
compose logs --no-log-prefix --since "$DEBUT" slice-service | grep "event=activation-queue" \
  | tee "$RESULTS/file-${STAMP}.txt" | tail -5 || true
