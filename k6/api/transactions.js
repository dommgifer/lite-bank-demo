// ============================================
// Transaction API（transaction-service）
// ============================================
import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL } from '../config/env.js';
import { getHeaders } from '../helpers/http.js';
import { errorRate } from '../helpers/metrics.js';

export function getTransactions(token, accountId) {
  const res = http.get(
    `${BASE_URL}/api/v1/transactions?accountId=${accountId}&page=0&size=10`,
    { headers: getHeaders(token) }
  );

  const success = check(res, {
    'get transactions successful': (r) => r.status === 200,
  });
  errorRate.add(success ? 0 : 1);
  return success;
}
