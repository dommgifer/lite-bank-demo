/**
 * OpenTelemetry Tracing 模組入口
 *
 * 使用方式：
 * 1. 在 main.jsx 中呼叫 initTracing() 初始化
 * 2. 在業務邏輯中使用 startSpan, endSpan 等函式追蹤操作
 */

// 導出初始化函式
export { initTracing, getTracer, shutdownTracing } from './tracer';

// 導出工具函式
export {
  startSpan,
  endSpan,
  setSpanAttributes,
  addSpanEvent,
  getCurrentSpan,
  getTraceHeaders,
  withSpan,
  maskAccount,
} from './utils';

// 導出配置
export { TRACING_CONFIG } from './config';
