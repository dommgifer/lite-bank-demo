// ============================================
// k6 測試場景配置
// ============================================

// 無限流量：DURATION 設為 'infinite' / '0' / 'inf' 時，換成 10 年(≈永久)。
// 搭配 K8s Deployment(restartPolicy Always)，process 若意外結束會自動重啟，
// 達成「持續一直有流量」的效果。
const rawDuration = (__ENV.DURATION || '5m').toLowerCase();
const INFINITE = ['infinite', 'inf', '0'].includes(rawDuration);
const DURATION = INFINITE ? '87600h' : (__ENV.DURATION || '5m');

export const options = {
  // setup 會逐一登入 TARGET_POOL 個用戶建轉帳目標池(預設整池 200)，
  // 拉高 setupTimeout 避免大池子時 setup 超時。
  setupTimeout: '180s',
  scenarios: {
    continuous_traffic: {
      executor: 'constant-vus',
      vus: parseInt(__ENV.VUS) || 10,
      duration: DURATION,
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<2000'], // 95% 請求在 2 秒內完成
    errors: ['rate<0.1'],              // 錯誤率低於 10%
  },
};
