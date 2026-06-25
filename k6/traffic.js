// ============================================
// Lite Bank 流量模擬測試 - 入口（K8s loadtest 用戶池版）
//
// 由 run-k8s.sh 傳入下列環境變數驅動：
//   MODE       normal | hot   情境模式
//   USER_POOL  loadtest 用戶池大小（對應 V10 種的 loadtest_0001..N）
//   HOT_USER   hot 模式集中壓的單一帳戶用戶
// normal：每個 VU 用 __VU 綁定不同 loadtest 用戶、分散打各自帳戶（低碰撞）。
// hot   ：所有 VU 都用 HOT_USER，集中壓同一帳戶、製造悲觀鎖爭用。
// ============================================
import http from 'k6/http';
import { options as scenarioOptions } from './config/options.js';
import { BASE_URL } from './config/env.js';
import { jsonHeaders } from './helpers/http.js';
import { login } from './api/auth.js';
import { fetchAccounts } from './api/accounts.js';
import { userSession } from './scenarios/userSession.js';

export const options = scenarioOptions;

const MODE = (__ENV.MODE || 'normal').toLowerCase();
const USER_POOL = parseInt(__ENV.USER_POOL) || 200;
const HOT_USER = __ENV.HOT_USER || 'loadtest_0001';

// loadtest 用戶帳密（密碼皆 password123，見 db/migration/V10）
function loadtestUser(n) {
  return { username: `loadtest_${String(n).padStart(4, '0')}`, password: 'password123' };
}

// 依 __VU 與 MODE 決定本次 iteration 綁定的用戶
function userForVU() {
  if (MODE === 'hot') {
    return { username: HOT_USER, password: 'password123' };
  }
  // normal：VU(1-based) 對映 loadtest 用戶，VUS > USER_POOL 時用 modulo 回繞
  const idx = ((__VU - 1) % USER_POOL) + 1;
  return loadtestUser(idx);
}

// ============================================
// Setup：建立轉帳目標池（按幣別分組的帳戶 id）
// 取樣一批 loadtest 用戶的帳戶當共用轉帳目標，只執行一次
// ============================================
export function setup() {
  console.log('========================================');
  console.log('Lite Bank 流量模擬測試');
  console.log(`目標: ${BASE_URL}`);
  console.log(`模式: ${MODE}`);
  console.log(`VUs: ${__ENV.VUS || 10}  Duration: ${__ENV.DURATION || '5m'}`);
  console.log(`用戶池: loadtest_0001..${String(USER_POOL).padStart(4, '0')}`);
  if (MODE === 'hot') console.log(`熱點用戶: ${HOT_USER}`);
  console.log('========================================');

  const healthCheck = http.get(`${BASE_URL}/api/v1/auth/login`, { headers: jsonHeaders });
  if (healthCheck.status === 0) {
    throw new Error(`無法連接到 ${BASE_URL}，請確認服務已啟動`);
  }

  // 取樣前 N 個 loadtest 用戶建立轉帳目標池（每人 4 幣別帳戶，少量取樣即可覆蓋）
  const sampleSize = Math.min(USER_POOL, 30);
  const transferTargets = {};

  for (let n = 1; n <= sampleSize; n++) {
    const loginResult = login(loadtestUser(n));
    if (!loginResult) continue;

    fetchAccounts(loginResult.token).forEach((acc) => {
      if (!transferTargets[acc.currency]) transferTargets[acc.currency] = [];
      transferTargets[acc.currency].push(acc.id);
    });
  }

  const summary = Object.fromEntries(
    Object.entries(transferTargets).map(([c, ids]) => [c, ids.length])
  );
  console.log(`Transfer targets (by currency): ${JSON.stringify(summary)}`);
  return { transferTargets, startTime: new Date().toISOString() };
}

// ============================================
// Default：每個 VU 反覆執行的 session
// ============================================
export default function ({ transferTargets }) {
  userSession(userForVU(), transferTargets);
}

// ============================================
// Teardown：測試結束後執行
// ============================================
export function teardown(data) {
  console.log('========================================');
  console.log('測試完成');
  console.log(`開始時間: ${data.startTime}`);
  console.log(`結束時間: ${new Date().toISOString()}`);
  console.log('========================================');
}
