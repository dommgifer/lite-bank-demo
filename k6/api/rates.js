// ============================================
// Exchange Rate API（exchange-rate-service）
// ============================================
import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL } from '../config/env.js';
import { getHeaders } from '../helpers/http.js';
import { errorRate } from '../helpers/metrics.js';

export function getExchangeRate(token, from, to) {
  const res = http.get(`${BASE_URL}/api/v1/rates/${from}/${to}`, {
    headers: getHeaders(token),
  });

  const success = check(res, {
    'get exchange rate successful': (r) => r.status === 200,
  });
  errorRate.add(success ? 0 : 1);
  return success;
}
