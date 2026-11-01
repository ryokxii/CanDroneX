// Mesure du §10.4 : 1 min de montée, puis 5 min à 600 opérations/s, par la passerelle.
// 60 % de consultations, 30 % de commandes, 10 % d'enregistrements de drones.
// Lancé par load-tests/mesurer-performance.sh ; les secrets viennent de l'environnement.
import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';

const KC = __ENV.KC || 'http://keycloak:8080';
const GW = __ENV.GW || 'http://api-gateway:8080';
const RATE = Number(__ENV.RATE || 600);
const RAMP = __ENV.RAMP || '1m';
const STEADY = __ENV.STEADY || '5m';
// Préfixe propre à l'exécution : les IMSI et ICCID restent uniques d'une mesure à l'autre.
const RUN = String(__ENV.RUN || Date.now() % 1000).padStart(3, '0');

export const options = {
  scenarios: {
    mixte: {
      executor: 'ramping-arrival-rate',
      startRate: 0, timeUnit: '1s',
      preAllocatedVUs: 200, maxVUs: 1000,
      stages: [{ target: RATE, duration: RAMP }, { target: RATE, duration: STEADY }],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_duration{operation:consultation}': ['p(95)<500'],
  },
  summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)', 'max'],
};

function fetchToken() {
  const r = http.post(`${KC}/realms/candronex/protocol/openid-connect/token`, {
    grant_type: 'client_credentials', client_id: 'inspectra-demo', client_secret: __ENV.CLIENT_SECRET,
  }, { tags: { operation: 'jeton' } });
  if (r.status !== 200) throw new Error(`jeton refusé par Keycloak : ${r.status}`);
  return r.json();
}

// Jeton de 300 s, test de 6 min : chaque VU le renouvelle avant son expiration.
let token = null;
let renewAt = 0;
function auth(extra) {
  if (Date.now() >= renewAt) {
    const t = fetchToken();
    token = t.access_token;
    renewAt = Date.now() + (t.expires_in - 30) * 1000;
  }
  return { headers: Object.assign({ Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }, extra || {}) };
}

function luhn(digits) {
  let sum = 0;
  for (let i = 0; i < digits.length; i++) {
    let d = Number(digits[digits.length - 1 - i]);
    if (i % 2 === 0) { d *= 2; if (d > 9) d -= 9; }
    sum += d;
  }
  return digits + ((10 - (sum % 10)) % 10);
}

function networkIdentity(n) {
  const serial = String(n).padStart(7, '0');
  return { imsi: `30272${RUN}${serial}`, iccid: luhn(`89302${RUN}0${serial}00`) };
}

function register(n, params, tags) {
  const id = networkIdentity(n);
  return http.post(`${GW}/api/v1/drones`, JSON.stringify({ ...id, simType: 'ESIM' }), Object.assign(params, { tags }));
}

export function setup() {
  token = fetchToken().access_token;
  const params = { headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' } };
  const drones = [];
  for (let i = 0; i < 20; i++) {
    const r = register(9_000_000 + i, Object.assign({}, params), { operation: 'preparation' });
    if (r.status !== 201) throw new Error(`préparation : drone refusé ${r.status} ${r.body}`);
    drones.push(r.json('droneId'));
  }
  return { drones };
}

export default function ({ drones }) {
  const pick = Math.random();
  const drone = drones[Math.floor(Math.random() * drones.length)];

  if (pick < 0.6) {
    const r = http.get(`${GW}/api/v1/drones/${drone}`, Object.assign(auth(), { tags: { operation: 'consultation' } }));
    check(r, { 'consultation 200': (x) => x.status === 200 });
  } else if (pick < 0.9) {
    const body = JSON.stringify({ orderLines: [
      { droneId: drone, serviceSpecificationId: 'C2_URLLC' },
      { droneId: drone, serviceSpecificationId: 'IMAGERY_EMBB' },
    ] });
    const key = `k6-${RUN}-${exec.scenario.iterationInTest}`;
    const r = http.post(`${GW}/api/v1/service-orders`, body,
        Object.assign(auth({ 'Idempotency-Key': key }), { tags: { operation: 'commande' } }));
    check(r, { 'commande 201': (x) => x.status === 201 });
  } else {
    const r = register(exec.scenario.iterationInTest, auth(), { operation: 'enregistrement' });
    check(r, { 'enregistrement 201': (x) => x.status === 201 });
  }
}
