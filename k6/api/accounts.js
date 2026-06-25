// ============================================
// Account API（account-service）
// ============================================
import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, SUPPORTED_CURRENCIES } from '../config/env.js';
import { getHeaders } from '../helpers/http.js';
import { errorRate, createAccountDuration } from '../helpers/metrics.js';
import { randomItem } from '../helpers/utils.js';

// 查詢當前用戶的所有帳戶，回傳 [{ id, currency }]
export function fetchAccounts(token) {
  const res = http.get(`${BASE_URL}/api/v1/accounts`, {
    headers: getHeaders(token),
  });

  const success = check(res, {
    'fetch accounts successful': (r) => r.status === 200,
  });

  if (!success) {
    errorRate.add(1);
    return [];
  }

  errorRate.add(0);
  const body = JSON.parse(res.body);
  return body.data.map((a) => ({ id: a.accountId, currency: a.currency }));
}

// 查詢單一帳戶餘額
export function getAccountBalance(token, accountId) {
  const res = http.get(`${BASE_URL}/api/v1/accounts/${accountId}/balance`, {
    headers: getHeaders(token),
  });

  const success = check(res, {
    'get balance successful': (r) => r.status === 200,
  });
  errorRate.add(success ? 0 : 1);
  return success;
}

// 為現有用戶建立新幣別帳戶，自動過濾已有幣別
// 成功回傳 { id, currency }，失敗或無可用幣別回傳 null
export function createAccount(token, userId, existingCurrencies) {
  const available = SUPPORTED_CURRENCIES.filter((c) => !existingCurrencies.includes(c));
  if (available.length === 0) return null;

  const currency = randomItem(available);
  const startTime = new Date();

  const res = http.post(
    `${BASE_URL}/api/v1/accounts`,
    JSON.stringify({ userId, currency }),
    { headers: getHeaders(token) }
  );

  createAccountDuration.add(new Date() - startTime);

  const success = check(res, {
    'create account successful': (r) => r.status === 201,
  });
  errorRate.add(success ? 0 : 1);

  if (!success) {
    console.log(`Create account failed: ${res.status} ${res.body}`);
    return null;
  }

  const body = JSON.parse(res.body);
  return { id: body.data.accountId, currency: body.data.currency };
}
