// ============================================
// Withdrawal API（teller-service）
// ============================================
import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL } from '../config/env.js';
import { getHeaders } from '../helpers/http.js';
import { errorRate } from '../helpers/metrics.js';
import { randomAmount } from '../helpers/utils.js';

export function withdraw(token, accountId, currency) {
  const amount = randomAmount(1, 50);
  const res = http.post(
    `${BASE_URL}/api/v1/withdrawals`,
    JSON.stringify({
      accountId,
      amount,
      currency,
      description: `k6 simulation ${new Date().toISOString()}`,
    }),
    { headers: getHeaders(token) }
  );

  const success = check(res, {
    'withdrawal successful': (r) => r.status === 200 || r.status === 201,
  });
  errorRate.add(success ? 0 : 1);
  return success;
}
