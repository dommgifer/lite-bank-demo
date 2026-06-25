// ============================================
// Deposit/Withdrawal API（deposit-withdrawal-service）
// ============================================
import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL } from '../config/env.js';
import { getHeaders } from '../helpers/http.js';
import { errorRate } from '../helpers/metrics.js';
import { randomAmount } from '../helpers/utils.js';

export function deposit(token, accountId, currency) {
  const amount = randomAmount(10, 100);
  const res = http.post(
    `${BASE_URL}/api/v1/deposits`,
    JSON.stringify({
      accountId,
      amount,
      currency,
      description: `k6 simulation ${new Date().toISOString()}`,
    }),
    { headers: getHeaders(token) }
  );

  const success = check(res, {
    'deposit successful': (r) => r.status === 200 || r.status === 201,
  });
  errorRate.add(success ? 0 : 1);
  return success;
}
