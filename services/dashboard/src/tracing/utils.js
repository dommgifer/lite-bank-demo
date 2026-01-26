/**
 * OpenTelemetry 工具函式
 */
import { trace, context, SpanStatusCode, propagation } from '@opentelemetry/api';
import { TRACING_CONFIG } from './config';
import { getTracer } from './tracer';

/**
 * 建立新的 span
 * @param {string} name - span 名稱
 * @param {Object} options - 選項
 * @param {Object} options.attributes - span 屬性
 * @param {Object} options.parent - 父 span (可選)
 * @returns {Object} span 實例
 */
export function startSpan(name, options = {}) {
  if (!TRACING_CONFIG.enabled) {
    return createNoopSpan();
  }

  const tracer = getTracer();
  if (!tracer) {
    return createNoopSpan();
  }

  try {
    const spanOptions = {};

    if (options.attributes) {
      spanOptions.attributes = options.attributes;
    }

    // 如果指定父 span，在其 context 中建立子 span
    if (options.parent) {
      const parentContext = trace.setSpan(context.active(), options.parent);
      return tracer.startSpan(name, spanOptions, parentContext);
    }

    return tracer.startSpan(name, spanOptions);
  } catch (error) {
    console.warn('[OTel] Failed to start span:', error);
    return createNoopSpan();
  }
}

/**
 * 結束 span
 * @param {Object} span - span 實例
 * @param {string} status - 狀態 ('OK' 或 'ERROR')
 * @param {string} errorMessage - 錯誤訊息（僅當 status 為 ERROR 時）
 */
export function endSpan(span, status = 'OK', errorMessage = null) {
  if (!span || span._isNoop) return;

  try {
    if (status === 'ERROR') {
      span.setStatus({
        code: SpanStatusCode.ERROR,
        message: errorMessage || 'Unknown error',
      });
      if (errorMessage) {
        span.recordException(new Error(errorMessage));
      }
    } else {
      span.setStatus({ code: SpanStatusCode.OK });
    }
    span.end();
  } catch (error) {
    console.warn('[OTel] Failed to end span:', error);
  }
}

/**
 * 設定 span 屬性
 * @param {Object} span - span 實例
 * @param {Object} attributes - 屬性鍵值對
 */
export function setSpanAttributes(span, attributes) {
  if (!span || span._isNoop || !attributes) return;

  try {
    Object.entries(attributes).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        span.setAttribute(key, value);
      }
    });
  } catch (error) {
    console.warn('[OTel] Failed to set span attributes:', error);
  }
}

/**
 * 新增 span 事件
 * @param {Object} span - span 實例
 * @param {string} name - 事件名稱
 * @param {Object} attributes - 事件屬性
 */
export function addSpanEvent(span, name, attributes = {}) {
  if (!span || span._isNoop) return;

  try {
    span.addEvent(name, attributes);
  } catch (error) {
    console.warn('[OTel] Failed to add span event:', error);
  }
}

/**
 * 取得當前 active span
 * @returns {Object|null} span 實例
 */
export function getCurrentSpan() {
  if (!TRACING_CONFIG.enabled) return null;
  return trace.getActiveSpan();
}

/**
 * 取得當前 trace context（用於注入 HTTP headers）
 * @param {Object} span - 可選的 span 實例，如果提供則從該 span 獲取 context
 * @returns {Object} 包含 traceparent 的 headers
 */
export function getTraceHeaders(span = null) {
  if (!TRACING_CONFIG.enabled) return {};

  const headers = {};
  try {
    // 如果有傳入 span，使用該 span 的 context；否則使用 active context
    const ctx = span
      ? trace.setSpan(context.active(), span)
      : context.active();
    propagation.inject(ctx, headers);
  } catch (error) {
    console.warn('[OTel] Failed to inject trace headers:', error);
  }
  return headers;
}

/**
 * 包裝非同步函式，自動建立和結束 span
 * @param {string} name - span 名稱
 * @param {Function} fn - 要執行的函式
 * @param {Object} options - span 選項
 * @returns {Promise} 函式執行結果
 */
export async function withSpan(name, fn, options = {}) {
  const span = startSpan(name, options);

  try {
    const result = await context.with(trace.setSpan(context.active(), span), fn);
    endSpan(span, 'OK');
    return result;
  } catch (error) {
    endSpan(span, 'ERROR', error.message);
    throw error;
  }
}

/**
 * 遮罩帳號（僅顯示後四碼）
 * @param {string} accountNumber - 完整帳號
 * @returns {string} 遮罩後的帳號
 */
export function maskAccount(accountNumber) {
  if (!accountNumber || accountNumber.length < 4) return '****';
  return '****' + accountNumber.slice(-4);
}

/**
 * 建立 No-op span（追蹤禁用時使用）
 */
function createNoopSpan() {
  return {
    _isNoop: true,
    setAttribute: () => {},
    setAttributes: () => {},
    addEvent: () => {},
    setStatus: () => {},
    recordException: () => {},
    end: () => {},
  };
}
