/**
 * OpenTelemetry 配置
 */
export const TRACING_CONFIG = {
  // 服務名稱
  serviceName: 'frontend-dashboard',
  serviceVersion: '1.0.0',

  // OTel Collector URL (HTTP)
  // 預設使用相對路徑，透過 nginx proxy 轉發到 collector
  collectorUrl: import.meta.env.VITE_OTEL_COLLECTOR_URL || '/v1/traces',

  // 是否啟用追蹤
  enabled: import.meta.env.VITE_OTEL_ENABLED !== 'false',

  // 是否為開發環境（啟用 console 輸出）
  debug: import.meta.env.DEV,

  // 批次處理配置
  batchConfig: {
    maxQueueSize: 100,
    maxExportBatchSize: 10,
    scheduledDelayMillis: 5000,
  }
};
