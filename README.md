# CanDroneX

Plateforme B2B de connectivité 5G pour exploitants de drones — Phase 2, microservices (LOG430).

| Cas d'utilisation | Service | État |
|---|---|---|
| UC-02 — Enregistrer un drone et son identité réseau | Drone Service | Implémenté |
| UC-04 — Commander les services de connectivité d'un drone | Order Service | Implémenté |
| UC-06 — Activer et configurer un slice | Slice Service | Implémenté, réseau 5G simulé |

## Architecture

```
Client B2B ──(1) client_credentials──► Keycloak :8081 (realm candronex, fourni)
     │
     │ (2) Authorization: Bearer <JWT>
     ▼
KrakenD :8080 ──► Drone Service ─► drone_db
     │            ▲ GET /api/v1/drones/{id}, jeton du client
     └──────────► Order Service ─► order_db (catalogue, commandes)
                  │ /internal/v1/service-activations, jeton de service order-service
                  ▼
                  Slice Service ─► slice_db (activation, inventaire)
                  └─ NetworkProvisioningPort → MockOpen5GSAdapter (dans le processus)
```

Seuls la passerelle (8080), Keycloak (8081) et Swagger UI (8090) sont publiés. Les services
et leurs bases ne sont joignables que sur le réseau `candronex-net`. Chaque service revalide
le jeton (signature, expiration, émetteur, audience `candronex-api`) et lit le client dans la
claim `azp` ; l'en-tête `X-Client-Id` n'est jamais lu.

## Prérequis

- Docker Desktop (ou Docker Engine avec Compose v2), en cours d'exécution.
- Les ports `8080`, `8081` et `8090` libres.

Java et Maven ne sont nécessaires que pour exécuter les tests hors conteneur (JDK 21 ou plus récent).

## Démarrer

```bash
cp .env.example .env
```

Remplacez chaque `changez-moi` par un secret de votre choix, puis :

```bash
docker compose up --build -d --wait
```

Keycloak importe le realm `candronex` et ses clients ; aucune configuration manuelle n'est
nécessaire. Avec `SPRING_PROFILES_ACTIVE=demo` (défaut), Flyway charge aussi le jeu de
démonstration (`db/demo/R__donnees_demo.sql`) : 4 drones, 4 commandes, 4 services.

## Démontrer

```bash
scripts/demo-phase2.sh
```

Le script obtient les jetons, vérifie la sécurité (401, jeton modifié ou non signé, 404
interclient, `X-Client-Id` forgé), puis enchaîne UC-02, UC-04 (201, rejeu 200, 409, 400) et
UC-06, dont il affiche le parcours dans les journaux.

Pour appeler l'API à la main :

```bash
TOKEN=$(curl -s -X POST http://localhost:8081/realms/candronex/protocol/openid-connect/token \
  -d grant_type=client_credentials -d client_id=inspectra-demo \
  -d client_secret="$INSPECTRA_DEMO_SECRET" | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')

curl -s -X POST http://localhost:8080/api/v1/drones \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"imsi":"302720123456700","iccid":"8930272000000000019","simType":"ESIM"}'

curl -s -X POST http://localhost:8080/api/v1/service-orders \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{"orderLines":[{"droneId":"<droneId>","serviceSpecificationId":"C2_URLLC"},
                    {"droneId":"<droneId>","serviceSpecificationId":"IMAGERY_EMBB"}]}'
```

Le jeton est valide 300 s. La consultation d'une commande (UC-05) est hors périmètre : l'état
final se lit dans les journaux.

## Contrats OpenAPI

Swagger UI : <http://localhost:8090>. Les contrats sont dans `docs/api/` : `gateway.yaml`
(contrat B2B), `drone-service.yaml`, `order-service.yaml`, `slice-service.yaml` (interne).

## Journaux

```bash
docker compose logs -f order-service slice-service
docker compose logs --no-color order-service | grep "orderId=<orderId>"
```

Chaque ligne porte l'horodatage UTC, `service`, `correlationId` (en-tête `X-Correlation-Id`,
transmis d'un service à l'autre) et l'événement : `order-received`, `activation-requested`,
`network-call`, `activation-result`, `order-status`, ou `status` et `error` pour une erreur.

## Réseau simulé

| Variable | Défaut | Effet |
|---|---|---|
| `MOCK_LATENCY_MS` | 1200 | Latence moyenne d'un appel (± 50 %) |
| `MOCK_FAILURE_RATE` | 0.1 | Part des activations refusées → `FAILED` |
| `MOCK_TIMEOUT_RATE` | 0.05 | Part des appels sans réponse dans le délai → `UNCONFIRMED` |
| `MOCK_TIMEOUT` | 5s | Délai d'un appel au réseau |

Un service `UNCONFIRMED` n'est pas un échec : toutes les 10 s, Slice Service lit son état réel
(`findStatus`) avant toute reprise, et ne reconfigure jamais un service déjà actif.

## Tests

```bash
mvn test     # tests unitaires et d'API, sans Docker
mvn verify   # ajoute les *IT, sur PostgreSQL 16 (Testcontainers) ; Docker requis
```

## Mesure de performance

```bash
MOCK_FAILURE_RATE=0.02 MOCK_TIMEOUT_RATE=0.01 docker compose up -d --wait
scripts/mesurer-performance.sh          # 1 min de montée, puis 5 min à 600 op/s
```

k6 s'exécute dans un conteneur sur `candronex-net` (60 % consultations, 30 % commandes,
10 % enregistrements). Le script calcule ensuite, dans `slice_db`, le P95 du délai entre
`acceptedAt` et `activatedAt` ; un service non actif à la fin compte comme dépassant 5 s.
Les résultats sont écrits dans `load-tests/resultats/`.

## Arrêter et réinitialiser

```bash
docker compose down       # conserve les volumes
docker compose down -v    # supprime les trois bases ; le prochain démarrage les recrée
```

## Organisation

```
platform/        code technique commun : validation du JWT, erreurs RFC 9457, corrélation
drone-service/   UC-02 — drone et identité réseau (IMSI, ICCID)
order-service/   UC-04 — catalogue (TMF633), commande (TMF641), appels à Drone et Slice Service
slice-service/   UC-06 — activation (TMF640), inventaire (TMF638), réseau simulé
gateway/         configuration KrakenD
keycloak/        realm candronex importé au démarrage
docs/api/        contrats OpenAPI 3.0.1
load-tests/      scénario k6 et calcul du P95
scripts/         démonstration et mesure
```

Chaque service suit la même structure hexagonale : `api` (adaptateurs entrants), `application`
(cas d'utilisation), `domain` et `domain/port` (modèle et ports, sans dépendance au cadriciel),
`infrastructure` (persistance JPA, clients REST, réseau simulé). Aucun service ne lit la base
d'un autre ; `platform` ne partage aucun type du domaine.

## Secrets

`.env` n'est jamais versionné ni livré : seul `.env.example` l'est. Le realm et les services
lisent les secrets dans l'environnement ; aucun secret ni jeton n'est écrit dans les journaux.
Pour produire une archive de remise sans fichier non suivi :

```bash
git archive --format=zip --output=candronex.zip HEAD
```
