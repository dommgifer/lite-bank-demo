/**
 * Axios 追蹤攔截器
 *
 * 功能：
 * 1. 為每個 HTTP 請求建立 span
 * 2. 自動注入 traceparent header
 * 3. 記錄請求/回應資訊
 */
import { startSpan, endSpan, setSpanAttributes, getTraceHeaders } from '../utils';
import { TRACING_CONFIG } from '../config';

// 儲存每個請求對應的 span
const requestSpanMap = new WeakMap();

/**
 * 建立請求攔截器
 */
export function createRequestInterceptor() {
  return (config) => {
    if (!TRACING_CONFIG.enabled) return config;

    try {
      // 取得 API endpoint path
      // 處理相對 URL：當 baseURL 也是相對路徑時，使用 window.location.origin 作為基礎
      let baseUrl = window.location.origin;
      if (config.baseURL) {
        // 如果 baseURL 是絕對路徑，直接使用；否則加上 origin
        baseUrl = config.baseURL.startsWith('http')
          ? config.baseURL
          : `${window.location.origin}${config.baseURL}`;
      }
      const url = new URL(config.url, baseUrl);
      const endpoint = url.pathname;

      // 建立 span
      const span = startSpan(`HTTP ${config.method?.toUpperCase()} ${endpoint}`, {
        attributes: {
          'http.method': config.method?.toUpperCase(),
          'http.url': url.href,
          'http.target': endpoint,
        },
      });

      // 儲存 span 以便在回應時使用
      requestSpanMap.set(config, span);

      // 注入 trace context 到 headers（傳入 span 以確保獲取正確的 context）
      const traceHeaders = getTraceHeaders(span);
      config.headers = {
        ...config.headers,
        ...traceHeaders,
      };

      // Debug: 輸出 trace headers
      console.log('[OTel] Injecting trace headers:', traceHeaders);
    } catch (error) {
      console.warn('[OTel] Failed to instrument request:', error);
    }

    return config;
  };
}

/**
 * 建立回應攔截器
 */
export function createResponseInterceptor() {
  return (response) => {
    if (!TRACING_CONFIG.enabled) return response;

    try {
      const span = requestSpanMap.get(response.config);
      if (span) {
        setSpanAttributes(span, {
          'http.status_code': response.status,
        });
        endSpan(span, 'OK');
        requestSpanMap.delete(response.config);
      }
    } catch (error) {
      console.warn('[OTel] Failed to complete response span:', error);
    }

    return response;
  };
}

/**
 * 建立錯誤攔截器
 */
export function createErrorInterceptor() {
  return (error) => {
    if (!TRACING_CONFIG.enabled) return Promise.reject(error);

    try {
      const config = error.config || error.request?.config;
      if (config) {
        const span = requestSpanMap.get(config);
        if (span) {
          const statusCode = error.response?.status || 0;
          setSpanAttributes(span, {
            'http.status_code': statusCode,
            'error.type': error.name || 'Error',
            'error.message': error.message,
          });
          endSpan(span, 'ERROR', error.message);
          requestSpanMap.delete(config);
        }
      }
    } catch (err) {
      console.warn('[OTel] Failed to complete error span:', err);
    }

    return Promise.reject(error);
  };
}

/**
 * 為 axios 實例註冊追蹤攔截器
 * @param {Object} axiosInstance - axios 實例
 */
export function registerAxiosTracing(axiosInstance) {
  if (!axiosInstance) {
    console.warn('[OTel] No axios instance provided');
    return;
  }

  // 註冊請求攔截器
  axiosInstance.interceptors.request.use(
    createRequestInterceptor(),
    (error) => Promise.reject(error)
  );

  // 註冊回應攔截器
  axiosInstance.interceptors.response.use(
    createResponseInterceptor(),
    createErrorInterceptor()
  );

  console.log('[OTel] Axios tracing interceptors registered');
}
