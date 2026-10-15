# CanDroneX

Plateforme B2B de connectivité 5G pour exploitants de drones — monolithe modulaire (LOG430).

Le dépôt contient le code, le `docker-compose.yml` qui démarre l'application et sa base,
les contrats d'API (`docs/api/openapi.yaml`) et les diagrammes d'architecture (`docs/diagrams/`).

| Cas d'utilisation | État |
|---|---|
| UC-02 — Enregistrer un drone et son identité réseau | Implémenté |
| UC-04 — Commander les services de connectivité d'un drone | Implémenté |
| UC-06 — Activer et configurer un slice | Modélisé (`docs/diagrams/modele-domaine.puml`), non implémenté |

## Prérequis

- Docker Desktop (ou Docker Engine avec Compose v2), en cours d'exécution.
- Le port `8080` libre sur le poste.

Java et Maven ne sont nécessaires que pour exécuter les tests hors conteneur (JDK 21 ou plus récent).

## Démarrer

Les secrets ne sont pas versionnés : créez le fichier d'environnement à partir du gabarit.

```bash
cp .env.example .env
```

Puis démarrez l'application et la base de données :

```bash
docker compose up --build
```

Au démarrage, Flyway applique les migrations `V1` à `V10` : schémas `customer`, `drone`,
`orders` et `catalog`, catalogue de référence (C2 / URLLC et Imagerie / eMBB), et client de
démonstration.

## Vérifier

```bash
docker compose ps
```

Les deux conteneurs doivent être à l'état `running (healthy)`. Puis, de bout en bout :

```bash
./scripts/demo.sh
```

Le script obtient un jeton, enregistre un drone (UC-02), commande ses deux services (UC-04),
rejoue la commande et vérifie les principaux refus. Il est rejouable : chaque exécution utilise
un nouvel IMSI et un nouvel ICCID d'essai (PLMN 999-70, réservé aux essais).

### Obtenir un jeton à la main

Le client de démonstration est amorcé par les migrations `V4` et `V5`.

| | |
|---|---|
| `client_id` | `0b6f1c2e-8f4a-4d3b-9c71-5e2a7d9f3b10` |
| `client_secret` | `inspectra-demo-secret` |

```bash
curl -s -X POST http://localhost:8080/oauth2/token \
  -u 0b6f1c2e-8f4a-4d3b-9c71-5e2a7d9f3b10:inspectra-demo-secret \
  -d grant_type=client_credentials
```

Le champ `access_token` de la réponse s'emploie ensuite dans l'en-tête
`Authorization: Bearer <jeton>` ; il est valide 15 minutes.

```bash
curl -s -X POST http://localhost:8080/api/v1/drones \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"imsi":"999700000010231","iccid":"8999700000000010239","simType":"ESIM"}'

curl -s -X POST http://localhost:8080/api/v1/service-orders \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{"orderLines":[{"droneId":"<droneId>","serviceSpecificationId":"C2_URLLC"}]}'
```

## Arrêter et réinitialiser

```bash
docker compose down
```

Les conteneurs sont supprimés, le volume `candronex-data` est conservé.

```bash
docker compose down -v
```

Supprime aussi le volume : au démarrage suivant, la base est recréée et les migrations rejouées.
C'est nécessaire si le volume contient des drones enregistrés avant `V7` (ajout de l'ICCID).

## Tests

```bash
mvn test
```

Tests unitaires et tests d'API (`*Test`), sans base ni conteneur : 119 tests.

```bash
mvn verify
```

Ajoute les tests d'intégration (`*IT`), exécutés par Failsafe sur un PostgreSQL 16 réel démarré
par Testcontainers : 20 tests. Docker doit être en cours d'exécution.

## Organisation

```
src/main/java/com/candronex/
  customermanagement/   client B2B, identifiant d'accès, jetons OAuth 2.0
  dronemanagement/      drone et identité réseau (IMSI, ICCID) — UC-02
  servicecatalog/       spécifications de service et caractéristiques (TMF633)
  ordermanagement/      commande, lignes, service commandé figé (TMF641) — UC-04
  common/               traduction des erreurs (RFC 9457), sécurité, horloge
```

Chaque module suit la même structure : `api` (adaptateurs entrants), `application` (services
applicatifs), `domain` (modèle, sans dépendance au cadriciel ni à l'ORM) et `domain/port`
(ports sortants), `infrastructure` (adaptateurs JPA), `published` (contrat offert aux autres
modules). Un module n'appelle un autre module que par son paquet `published`.

## Secrets

`.env` n'est jamais versionné ni livré : seul `.env.example` l'est. Pour produire une archive de
remise sans fichier non suivi, utilisez :

```bash
git archive --format=zip --output=candronex.zip HEAD
```

Le secret `inspectra-demo-secret` est un secret de démonstration, publié volontairement dans ce
README et dans la migration `V5`. Un environnement réel ne doit jamais démarrer avec lui : la
rotation des identifiants relève d'UC-01, hors périmètre.
