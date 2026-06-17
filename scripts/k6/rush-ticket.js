import http from 'k6/http';
import { check, sleep } from 'k6';

http.setResponseCallback(http.expectedStatuses({ min: 200, max: 399 }, 409, 429, 503));

export const options = {
  scenarios: {
    rush_ticket: {
      executor: 'ramping-vus',
      stages: [
        { duration: '10s', target: Number(__ENV.VUS || 100) },
        { duration: __ENV.DURATION || '30s', target: Number(__ENV.VUS || 100) },
        { duration: '10s', target: 0 },
      ],
      gracefulRampDown: '5s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<500'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const STRATEGY = __ENV.STRATEGY || 'REDIS_LUA';
const SKU_ID = Number(__ENV.SKU_ID || 1001);
const EVENT_ID = Number(__ENV.EVENT_ID || 3001);
const STOCK = Number(__ENV.STOCK || 100000);
const QUANTITY = Number(__ENV.QUANTITY || 1);
const USER_BASE = Number(__ENV.USER_BASE || 200000);

export function setup() {
  const payload = JSON.stringify({
    skuId: SKU_ID,
    totalStock: STOCK,
  });
  const response = http.post(`${BASE_URL}/api/rush/inventory/preload`, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { api: 'preload_inventory' },
  });
  check(response, {
    'preload status is 200': (res) => res.status === 200,
    'preload success': (res) => JSON.parse(res.body).success === true,
  });
}

export default function () {
  const requestId = `${Date.now()}-${__VU}-${__ITER}`;
  const userId = USER_BASE + (__VU * 1000000) + __ITER;
  const payload = JSON.stringify({
    requestId,
    userId,
    eventId: EVENT_ID,
    skuId: SKU_ID,
    quantity: QUANTITY,
    strategy: STRATEGY,
    idempotentKey: `rush:${STRATEGY}:${userId}:${SKU_ID}:${requestId}`,
  });

  const response = http.post(`${BASE_URL}/api/rush/tickets`, payload, {
    headers: { 'Content-Type': 'application/json' },
    tags: { api: 'rush_ticket', strategy: STRATEGY },
  });

  check(response, {
    'rush status is 2xx or business conflict': (res) => [200, 409, 429, 503].includes(res.status),
    'rush response has success flag': (res) => JSON.parse(res.body).success !== undefined,
  });

  sleep(Number(__ENV.SLEEP || 0.01));
}
