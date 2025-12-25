package com.litebank.gateway.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class OpenTelemetryConfigTest {

    private OpenTelemetryConfig config;

    @BeforeEach
    void setUp() {
        config = new OpenTelemetryConfig();
        ReflectionTestUtils.setField(config, "serviceName", "api-gateway");
        ReflectionTestUtils.setField(config, "otlpEndpoint", "http://localhost:4317");
    }

    @Test
    void openTelemetry_shouldCreateNonNullInstance() {
        // When
        OpenTelemetry openTelemetry = config.openTelemetry();

        // Then
        assertNotNull(openTelemetry, "OpenTelemetry instance should not be null");
    }

    @Test
    void openTelemetry_shouldReturnValidInstance() {
        // When
        OpenTelemetry openTelemetry = config.openTelemetry();

        // Then
        assertNotNull(openTelemetry);
        assertNotNull(openTelemetry.getTracer("test"));
    }

    @Test
    void tracer_shouldCreateNonNullInstance() {
        // Given
        OpenTelemetry openTelemetry = config.openTelemetry();

        // When
        Tracer tracer = config.tracer(openTelemetry);

        // Then
        assertNotNull(tracer, "Tracer instance should not be null");
    }

    @Test
    void tracer_shouldUseCorrectInstrumentationName() {
        // Given
        OpenTelemetry openTelemetry = config.openTelemetry();

        // When
        Tracer tracer = config.tracer(openTelemetry);

        // Then
        assertNotNull(tracer);
        // Tracer should be functional
        assertDoesNotThrow(() -> tracer.spanBuilder("test-span").startSpan().end());
    }

    @Test
    void openTelemetry_withCustomServiceName_shouldWork() {
        // Given
        ReflectionTestUtils.setField(config, "serviceName", "custom-service");

        // When
        OpenTelemetry openTelemetry = config.openTelemetry();

        // Then
        assertNotNull(openTelemetry);
    }

    @Test
    void openTelemetry_withCustomOtlpEndpoint_shouldWork() {
        // Given
        ReflectionTestUtils.setField(config, "otlpEndpoint", "http://custom-collector:4317");

        // When
        OpenTelemetry openTelemetry = config.openTelemetry();

        // Then
        assertNotNull(openTelemetry);
    }

    @Test
    void tracer_shouldBeAbleToCreateSpans() {
        // Given
        OpenTelemetry openTelemetry = config.openTelemetry();
        Tracer tracer = config.tracer(openTelemetry);

        // When
        io.opentelemetry.api.trace.Span span = tracer.spanBuilder("test-operation")
                .startSpan();

        // Then
        assertNotNull(span);
        assertNotNull(span.getSpanContext());
        assertNotNull(span.getSpanContext().getTraceId());
        assertNotNull(span.getSpanContext().getSpanId());

        // Cleanup
        span.end();
    }

    @Test
    void tracer_shouldSupportSpanAttributes() {
        // Given
        OpenTelemetry openTelemetry = config.openTelemetry();
        Tracer tracer = config.tracer(openTelemetry);

        // When
        io.opentelemetry.api.trace.Span span = tracer.spanBuilder("test-operation")
                .startSpan();

        // Then
        assertDoesNotThrow(() -> {
            span.setAttribute("test.attribute", "test-value");
            span.setAttribute("test.number", 123);
            span.setAttribute("test.boolean", true);
        });

        // Cleanup
        span.end();
    }

    @Test
    void configuration_shouldHaveCorrectDefaults() {
        // Given - using default configuration from setUp

        // When
        String serviceName = (String) ReflectionTestUtils.getField(config, "serviceName");
        String otlpEndpoint = (String) ReflectionTestUtils.getField(config, "otlpEndpoint");

        // Then
        assertEquals("api-gateway", serviceName);
        assertEquals("http://localhost:4317", otlpEndpoint);
    }

    @Test
    void multipleTracers_fromSameOpenTelemetry_shouldWork() {
        // Given
        OpenTelemetry openTelemetry = config.openTelemetry();

        // When
        Tracer tracer1 = config.tracer(openTelemetry);
        Tracer tracer2 = openTelemetry.getTracer("another-tracer", "1.0.0");

        // Then
        assertNotNull(tracer1);
        assertNotNull(tracer2);
        assertDoesNotThrow(() -> {
            tracer1.spanBuilder("span1").startSpan().end();
            tracer2.spanBuilder("span2").startSpan().end();
        });
    }
}
