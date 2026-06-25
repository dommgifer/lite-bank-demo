// ============================================
// Transfer API（transfer-service）
// ============================================
import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL } from '../config/env.js';
import { getHeaders } from '../helpers/http.js';
import { errorRate, transferDuration } from '../helpers/metrics.js';
import { randomItem, randomAmount } from '../helpers/utils.js';

// 從 transferTargets 池中選一個不同的同幣別帳戶進行轉帳
export function transfer(token, fromAccountId, currency, transferTargets) {
  const targets = (transferTargets[currency] || []).filter((id) => id !== fromAccountId);
  if (targets.length === 0) return false;

  const toAccountId = randomItem(targets);
  const amount = randomAmount(1, 10);

  const startTime = new Date();
  const res = http.post(
    `${BASE_URL}/api/v1/transfers`,
    JSON.stringify({
      fromAccountId,
      toAccountId,
      amount,
      currency,
      description: `k6 simulation ${new Date().toISOString()}`,
    }),
    { headers: getHeaders(token) }
  );

  transferDuration.add(new Date() - startTime);

  const success = check(res, {
    'transfer successful': (r) => r.status === 200 || r.status === 201,
  });

  if (!success) {
    console.log(`Transfer failed: ${res.status} ${res.body}`);
  }

  errorRate.add(success ? 0 : 1);
  return success;
}
