// ============================================
// Exchange API（exchange-service）
// ============================================
import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, RATE_CURRENCIES } from '../config/env.js';
import { getHeaders } from '../helpers/http.js';
import { errorRate } from '../helpers/metrics.js';
import { randomItem, randomAmount } from '../helpers/utils.js';

export function exchange(token, accounts) {
  // 從帳戶清單中找出兩個不同幣別的帳戶進行換匯
  if (accounts.length < 2) return false;

  const sourceAccount = randomItem(accounts);
  const otherAccounts = accounts.filter(
    (a) => a.currency !== sourceAccount.currency
  );
  if (otherAccounts.length === 0) return false;

  const destAccount = randomItem(otherAccounts);
  const amount = randomAmount(1, 20);

  const res = http.post(
    `${BASE_URL}/api/v1/exchanges`,
    JSON.stringify({
      sourceAccountId: sourceAccount.id,
      destinationAccountId: destAccount.id,
      amount,
      sourceCurrency: sourceAccount.currency,
      destinationCurrency: destAccount.currency,
      description: `k6 simulation ${new Date().toISOString()}`,
    }),
    { headers: getHeaders(token) }
  );

  const success = check(res, {
    'exchange successful': (r) => r.status === 200 || r.status === 201,
  });
  errorRate.add(success ? 0 : 1);
  return success;
}
