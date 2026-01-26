/**
 * OpenTelemetry Tracer Provider 設定
 * 適用於 @opentelemetry/sdk-trace-web v2.x
 */
import { WebTracerProvider, BatchSpanProcessor, SimpleSpanProcessor, ConsoleSpanExporter } from '@opentelemetry/sdk-trace-web';
import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-http';
import { resourceFromAttributes } from '@opentelemetry/resources';
import { ATTR_SERVICE_NAME, ATTR_SERVICE_VERSION } from '@opentelemetry/semantic-conventions';
import { W3CTraceContextPropagator } from '@opentelemetry/core';
import { trace, propagation } from '@opentelemetry/api';
import { TRACING_CONFIG } from './config';

let provider = null;
let tracer = null;

/**
 * 初始化 OpenTelemetry
 */
export function initTracing() {
  if (!TRACING_CONFIG.enabled) {
    console.log('[OTel] Tracing is disabled');
    return null;
  }

  if (provider) {
    console.log('[OTel] Already initialized');
    return tracer;
  }

  try {
    // 建立 Resource
    const resource = resourceFromAttributes({
      [ATTR_SERVICE_NAME]: TRACING_CONFIG.serviceName,
      [ATTR_SERVICE_VERSION]: TRACING_CONFIG.serviceVersion,
    });

    // 配置 OTLP Exporter
    const otlpExporter = new OTLPTraceExporter({
      url: TRACING_CONFIG.collectorUrl,
    });

    // 建立 span processors 陣列
    const spanProcessors = [
      new BatchSpanProcessor(otlpExporter, {
        maxQueueSize: TRACING_CONFIG.batchConfig.maxQueueSize,
        maxExportBatchSize: TRACING_CONFIG.batchConfig.maxExportBatchSize,
        scheduledDelayMillis: TRACING_CONFIG.batchConfig.scheduledDelayMillis,
      }),
    ];

    // 開發環境額外啟用 Console Exporter
    if (TRACING_CONFIG.debug) {
      spanProcessors.push(new SimpleSpanProcessor(new ConsoleSpanExporter()));
    }

    // 建立 TracerProvider（v2.x API：span processors 在構造函數中傳入）
    provider = new WebTracerProvider({
      resource,
      spanProcessors,
    });

    // 註冊 provider 並設定 propagator
    provider.register({
      propagator: new W3CTraceContextPropagator(),
    });

    // 設定全域 propagator
    propagation.setGlobalPropagator(new W3CTraceContextPropagator());

    // 取得 tracer
    tracer = trace.getTracer(TRACING_CONFIG.serviceName, TRACING_CONFIG.serviceVersion);

    console.log(`[OTel] OpenTelemetry initialized - service: ${TRACING_CONFIG.serviceName}`);
    console.log(`[OTel] Collector URL: ${TRACING_CONFIG.collectorUrl}`);

    return tracer;
  } catch (error) {
    console.warn('[OTel] Failed to initialize OpenTelemetry:', error);
    return null;
  }
}

/**
 * 取得 Tracer 實例
 */
export function getTracer() {
  if (!tracer && TRACING_CONFIG.enabled) {
    initTracing();
  }
  return tracer;
}

/**
 * 關閉 TracerProvider
 */
export async function shutdownTracing() {
  if (provider) {
    await provider.shutdown();
    provider = null;
    tracer = null;
    console.log('[OTel] TracerProvider shutdown');
  }
}
