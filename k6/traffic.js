import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// ============================================
// 配置
// ============================================
const BASE_URL = __ENV.BASE_URL || 'http://localhost:9000';

// 自訂指標
const errorRate = new Rate('errors');
const loginDuration = new Trend('login_duration');
const transferDuration = new Trend('transfer_duration');

// ============================================
// 測試場景配置
// ============================================
export const options = {
  scenarios: {
    // 預設場景：持續流量
    continuous_traffic: {
      executor: 'constant-vus',
      vus: parseInt(__ENV.VUS) || 10,
      duration: __ENV.DURATION || '5m',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<2000'], // 95% 請求在 2 秒內完成
    errors: ['rate<0.1'],              // 錯誤率低於 10%
  },
};

// ============================================
// 測試資料
// ============================================
const USERS = [
  {
    username: 'alice',
    password: 'password123',
    userId: 1,
    accounts: [
      { id: 1, currency: 'USD' },
      { id: 2, currency: 'EUR' },
      { id: 3, currency: 'TWD' },
    ],
  },
  {
    username: 'bob',
    password: 'password123',
    userId: 2,
    accounts: [
      { id: 4, currency: 'USD' },
      { id: 5, currency: 'JPY' },
    ],
  },
  {
    username: 'charlie',
    password: 'password123',
    userId: 3,
    accounts: [
      { id: 6, currency: 'USD' },
      { id: 7, currency: 'EUR' },
    ],
  },
];

// 所有 USD 帳戶（用於轉帳）
const USD_ACCOUNTS = [1, 4, 6];

// ============================================
// 輔助函數
// ============================================
function randomItem(array) {
  return array[Math.floor(Math.random() * array.length)];
}

function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function randomAmount(min, max) {
  return (Math.random() * (max - min) + min).toFixed(2);
}

function getHeaders(token) {
  return {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };
}

// ============================================
// API 操作
// ============================================

// 登入
function login(user) {
  const startTime = new Date();
  const res = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({
      username: user.username,
      password: user.password,
    }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  loginDuration.add(new Date() - startTime);

  const success = check(res, {
    'login successful': (r) => r.status === 200,
    'has token': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.data && body.data.token;
      } catch (e) {
        return false;
      }
    },
  });

  if (!success) {
    errorRate.add(1);
    console.log(`Login failed for ${user.username}: ${res.status} ${res.body}`);
    return null;
  }

  errorRate.add(0);
  const body = JSON.parse(res.body);
  return body.data.token;
}

// 查詢帳戶列表
function getAccounts(token) {
  const res = http.get(`${BASE_URL}/api/v1/accounts`, {
    headers: getHeaders(token),
  });

  const success = check(res, {
    'get accounts successful': (r) => r.status === 200,
  });
  errorRate.add(success ? 0 : 1);
  return success;
}

// 查詢單一帳戶餘額
function getAccountBalance(token, accountId) {
  const res = http.get(`${BASE_URL}/api/v1/accounts/${accountId}/balance`, {
    headers: getHeaders(token),
  });

  const success = check(res, {
    'get balance successful': (r) => r.status === 200,
  });
  errorRate.add(success ? 0 : 1);
  return success;
}

// 查詢交易歷史
function getTransactions(token, accountId) {
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

// 查詢匯率
function getExchangeRate(token, from, to) {
  const res = http.get(`${BASE_URL}/api/v1/rates/${from}/${to}`, {
    headers: getHeaders(token),
  });

  const success = check(res, {
    'get exchange rate successful': (r) => r.status === 200,
  });
  errorRate.add(success ? 0 : 1);
  return success;
}

// 轉帳（只在 USD 帳戶間轉帳，確保幣別一致）
function transfer(token, fromAccountId) {
  // 選擇一個不同的 USD 帳戶作為目標
  const otherAccounts = USD_ACCOUNTS.filter((id) => id !== fromAccountId);
  const toAccountId = randomItem(otherAccounts);
  const amount = randomAmount(1, 10); // 小額轉帳，避免餘額不足

  const startTime = new Date();
  const res = http.post(
    `${BASE_URL}/api/v1/transfers`,
    JSON.stringify({
      fromAccountId: fromAccountId,
      toAccountId: toAccountId,
      amount: amount,
      currency: 'USD',
      description: `k6 test transfer ${new Date().toISOString()}`,
    }),
    { headers: getHeaders(token) }
  );

  transferDuration.add(new Date() - startTime);

  const success = check(res, {
    'transfer successful': (r) => r.status === 200 || r.status === 201,
  });

  if (!success) {
    // 轉帳失敗可能是餘額不足，這是預期的
    console.log(`Transfer failed: ${res.status} ${res.body}`);
  }

  errorRate.add(success ? 0 : 1);
  return success;
}

// 存款
function deposit(token, accountId, currency) {
  const amount = randomAmount(10, 100);

  const res = http.post(
    `${BASE_URL}/api/v1/deposits`,
    JSON.stringify({
      accountId: accountId,
      amount: amount,
      currency: currency,
      description: `k6 test deposit ${new Date().toISOString()}`,
    }),
    { headers: getHeaders(token) }
  );

  const success = check(res, {
    'deposit successful': (r) => r.status === 200 || r.status === 201,
  });
  errorRate.add(success ? 0 : 1);
  return success;
}

// ============================================
// 主測試邏輯
// ============================================
export default function () {
  // 1. 隨機選擇一個用戶
  const user = randomItem(USERS);

  // 2. 登入
  const token = login(user);
  if (!token) {
    sleep(1);
    return;
  }

  // 3. 模擬用戶行為：執行 3-6 個隨機操作
  const numActions = randomInt(3, 6);

  for (let i = 0; i < numActions; i++) {
    const action = randomInt(1, 100);
    const account = randomItem(user.accounts);

    if (action <= 30) {
      // 30%: 查詢帳戶列表
      getAccounts(token);
    } else if (action <= 55) {
      // 25%: 查詢餘額
      getAccountBalance(token, account.id);
    } else if (action <= 75) {
      // 20%: 查詢交易歷史
      getTransactions(token, account.id);
    } else if (action <= 85) {
      // 10%: 查詢匯率
      const currencies = ['USD', 'EUR', 'TWD', 'JPY'];
      const from = randomItem(currencies);
      let to = randomItem(currencies);
      while (to === from) {
        to = randomItem(currencies);
      }
      getExchangeRate(token, from, to);
    } else if (action <= 93) {
      // 8%: 存款（補充餘額）
      deposit(token, account.id, account.currency);
    } else {
      // 7%: 轉帳（只有 USD 帳戶才轉帳）
      const usdAccount = user.accounts.find((a) => a.currency === 'USD');
      if (usdAccount) {
        transfer(token, usdAccount.id);
      } else {
        // 沒有 USD 帳戶就查詢餘額
        getAccountBalance(token, account.id);
      }
    }

    // 模擬人類思考時間 (1-3 秒)
    sleep(randomInt(1, 3));
  }

  // 4. 結束前的等待（模擬 session 結束）
  sleep(randomInt(1, 2));
}

// ============================================
// 生命週期 Hooks
// ============================================
export function setup() {
  console.log('========================================');
  console.log('Lite Bank 流量模擬測試');
  console.log(`目標: ${BASE_URL}`);
  console.log(`VUs: ${__ENV.VUS || 10}`);
  console.log(`Duration: ${__ENV.DURATION || '5m'}`);
  console.log('========================================');

  // 驗證服務是否可用
  const res = http.get(`${BASE_URL}/api/v1/auth/login`, {
    headers: { 'Content-Type': 'application/json' },
  });

  if (res.status === 0) {
    throw new Error(`無法連接到 ${BASE_URL}，請確認服務已啟動`);
  }

  return { startTime: new Date().toISOString() };
}

export function teardown(data) {
  console.log('========================================');
  console.log('測試完成');
  console.log(`開始時間: ${data.startTime}`);
  console.log(`結束時間: ${new Date().toISOString()}`);
  console.log('========================================');
}
