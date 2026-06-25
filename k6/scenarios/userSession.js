// ============================================
// 用戶 Session 情境（一般情境 / normal）
// 每個 VU 由 traffic.js 用 __VU 綁定一個固定 loadtest 用戶，
// 模擬真實用戶：login → 查自己帳戶 → 執行 3-6 個隨機操作(都打自己的帳戶)。
// 因為每個 VU 是不同用戶、不同帳戶，整體呈現真實的低碰撞分散流量。
// ============================================
import { sleep } from 'k6';
import { RATE_CURRENCIES } from '../config/env.js';
import { randomItem, randomInt } from '../helpers/utils.js';
import { login } from '../api/auth.js';
import { fetchAccounts, getAccountBalance, createAccount } from '../api/accounts.js';
import { getTransactions } from '../api/transactions.js';
import { transfer } from '../api/transfers.js';
import { deposit } from '../api/deposits.js';
import { withdraw } from '../api/withdrawals.js';
import { exchange } from '../api/exchanges.js';
import { getExchangeRate } from '../api/rates.js';

// 操作機率分配（累積值）
//   27% 查餘額 / 22% 交易歷史 / 12% 匯率 / 11% 存款 / 8% 提款
//   8% 轉帳 / 5% 換匯 / 7% 建新帳戶
export function userSession(user, transferTargets) {
  // 1. 登入綁定的用戶(每次 iteration 重新登入，避免長測 JWT 過期)
  const loginResult = login(user);
  if (!loginResult) { sleep(1); return; }
  const { token, userId } = loginResult;

  // 2. 查詢自己的帳戶清單（本次 session 的操作基礎）
  const accounts = fetchAccounts(token);
  if (accounts.length === 0) { sleep(1); return; }

  // 3. 執行 3-6 個隨機操作（都打自己的帳戶）
  const numActions = randomInt(3, 6);

  for (let i = 0; i < numActions; i++) {
    const action = randomInt(1, 100);
    const account = randomItem(accounts);

    if (action <= 27) {
      // 27%: 查詢餘額
      getAccountBalance(token, account.id);

    } else if (action <= 49) {
      // 22%: 查詢交易歷史
      getTransactions(token, account.id);

    } else if (action <= 61) {
      // 12%: 查詢匯率
      const from = randomItem(RATE_CURRENCIES);
      let to = randomItem(RATE_CURRENCIES);
      while (to === from) { to = randomItem(RATE_CURRENCIES); }
      getExchangeRate(token, from, to);

    } else if (action <= 72) {
      // 11%: 存款
      deposit(token, account.id, account.currency);

    } else if (action <= 80) {
      // 8%: 提款
      withdraw(token, account.id, account.currency);

    } else if (action <= 88) {
      // 8%: 轉帳（需有同幣別目標帳戶）
      const validTargets = (transferTargets[account.currency] || []).filter((id) => id !== account.id);
      if (validTargets.length > 0) {
        transfer(token, account.id, account.currency, transferTargets);
      } else {
        getAccountBalance(token, account.id); // fallback
      }

    } else if (action <= 93) {
      // 5%: 換匯（需有不同幣別帳戶）
      exchange(token, accounts);

    } else {
      // 7%: 建立新帳戶（即時查詢確保幣別不重複，新帳戶當次可用）
      const existingCurrencies = accounts.map((a) => a.currency);
      const newAccount = createAccount(token, userId, existingCurrencies);
      if (newAccount) {
        accounts.push(newAccount);
      }
    }

    sleep(randomInt(1, 3));
  }

  sleep(randomInt(1, 2));
}
