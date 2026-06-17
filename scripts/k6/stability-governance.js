import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';

http.setResponseCallback(http.expectedStatuses({ min: 200, max: 399 }, 409, 429, 503));

export const options = {
  scenarios: {
    stability_governance: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 300),
      duration: __ENV.DURATION || '60s',
      gracefulStop: '10s',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<1000'],
    unexpected_response_rate: ['rate<0.01'],
  },
};

const accepted = new Counter('rush_accepted');
const rateLimited = new Counter('rush_rate_limited');
const stockNotEnough = new Counter('rush_stock_not_enough');
const idempotentConflict = new Counter('rush_idempotent_conflict');
const serviceDegraded = new Counter('rush_service_degraded');
const unexpectedResponses = new Counter('rush_unexpected_responses');
const unexpectedResponseRate = new Rate('unexpected_response_rate');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const STRATEGY = __ENV.STRATEGY || 'REDIS_LUA';
const SKU_ID = Number(__ENV.SKU_ID || 1001);
const SKU_SPREAD = Number(__ENV.SKU_SPREAD || 1);
const EVENT_ID = Number(__ENV.EVENT_ID || 3001);
const STOCK = Number(__ENV.STOCK || 1000000);
const QUANTITY = Number(__ENV.QUANTITY || 1);
const USER_BASE = Number(__ENV.USER_BASE || 900000);
const SCENARIO_TAG = __ENV.SCENARIO_TAG || 'governed';
const SLEEP_SECONDS = Number(__ENV.SLEEP || 0);

export function setup() {
  for (let offset = 0; offset < SKU_SPREAD; offset += 1) {
    const payload = JSON.stringify({
      skuId: SKU_ID + offset,
      totalStock: STOCK,
    });
    const response = http.post(`${BASE_URL}/api/rush/inventory/preload`, payload, {
      headers: { 'Content-Type': 'application/json' },
      tags: { api: 'preload_inventory', scenario: SCENARIO_TAG },
    });
    check(response, {
      'preload status is 200': (res) => res.status === 200,
      'preload success': (res) => parseBody(res).success === true,
    });
  }
}

export default function () {
  const skuId = SKU_ID + ((__VU + __ITER) % SKU_SPREAD);
  const requestId = `${SCENARIO_TAG}-${Date.now()}-${__VU}-${__ITER}`;
  const userId = USER_BASE + (__VU * 1000000) + __ITER;
  const payload = JSON.stringify({
    requestId,
    userId,
    eventId: EVENT_ID,
    skuId,
    quantity: QUANTITY,
    strategy: STRATEGY,
    idempotentKey: `rush:${SCENARIO_TAG}:${STRATEGY}:${userId}:${skuId}:${requestId}`,
  });

  const response = http.post(`${BASE_URL}/api/rush/tickets`, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: {
      api: 'rush_ticket',
      scenario: SCENARIO_TAG,
      strategy: STRATEGY,
      sku: String(skuId),
    },
  });
  const body = parseBody(response);
  const categorized = recordResult(response, body);

  check(response, {
    'response status is known': (res) => [200, 409, 429, 503].includes(res.status),
    'response code is categorized': () => categorized,
  });
  unexpectedResponseRate.add(!categorized);

  if (SLEEP_SECONDS > 0) {
    sleep(SLEEP_SECONDS);
  }
}

function recordResult(response, body) {
  if (response.status === 200 && body.success === true) {
    accepted.add(1);
    return true;
  }

  switch (body.code) {
    case 'C0429':
      rateLimited.add(1);
      return true;
    case 'B0401':
    case 'B0402':
      stockNotEnough.add(1);
      return true;
    case 'A0429':
      idempotentConflict.add(1);
      return true;
    case 'C0503':
      serviceDegraded.add(1);
      return true;
    default:
      unexpectedResponses.add(1);
      return false;
  }
}

function parseBody(response) {
  try {
    return JSON.parse(response.body || '{}');
  } catch (error) {
    return {};
  }
}
