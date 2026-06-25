// ============================================
// k6 測試場景配置
// ============================================

export const options = {
  scenarios: {
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
