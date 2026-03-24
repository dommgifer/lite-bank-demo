{{/*
Expand the name of the chart.
*/}}
{{- define "litebank.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "litebank.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "litebank.labels" -}}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version | replace "+" "_" }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: litebank
{{- end }}

{{/*
Selector labels for a service
*/}}
{{- define "litebank.selectorLabels" -}}
app.kubernetes.io/name: {{ .name }}
app.kubernetes.io/instance: {{ .root.Release.Name }}
{{- end }}

{{/* ===========================================
    Infrastructure Endpoints
    =========================================== */}}

{{/*
PostgreSQL Host
*/}}
{{- define "litebank.postgresHost" -}}
{{- if .Values.infrastructure.postgres.enabled -}}
postgres
{{- else -}}
{{ .Values.infrastructure.postgres.external.host }}
{{- end -}}
{{- end }}

{{/*
PostgreSQL Port
*/}}
{{- define "litebank.postgresPort" -}}
{{- if .Values.infrastructure.postgres.enabled -}}
5432
{{- else -}}
{{ .Values.infrastructure.postgres.external.port }}
{{- end -}}
{{- end }}

{{/*
Kafka Bootstrap Servers
*/}}
{{- define "litebank.kafkaServers" -}}
{{- if .Values.infrastructure.kafka.enabled -}}
kafka:9092
{{- else -}}
{{ .Values.infrastructure.kafka.external.bootstrapServers }}
{{- end -}}
{{- end }}

{{/* ===========================================
    Observability Endpoints
    =========================================== */}}

{{/*
OTel Collector Endpoint
*/}}
{{- define "litebank.otelEndpoint" -}}
{{- if .Values.observability.otelCollector.enabled -}}
http://otel-collector:4318
{{- else -}}
{{ required "observability.otelCollector.external.endpoint is required when otelCollector.enabled=false" .Values.observability.otelCollector.external.endpoint }}
{{- end -}}
{{- end }}

{{/*
Tempo Endpoint
*/}}
{{- define "litebank.tempoEndpoint" -}}
{{- if .Values.observability.tempo.enabled -}}
http://tempo:4318
{{- else -}}
{{ required "observability.tempo.external.endpoint is required when tempo.enabled=false" .Values.observability.tempo.external.endpoint }}
{{- end -}}
{{- end }}

{{/*
Loki Endpoint
*/}}
{{- define "litebank.lokiEndpoint" -}}
{{- if .Values.observability.loki.enabled -}}
http://loki:3100
{{- else -}}
{{ required "observability.loki.external.endpoint is required when loki.enabled=false" .Values.observability.loki.external.endpoint }}
{{- end -}}
{{- end }}

{{/*
Prometheus Endpoint
*/}}
{{- define "litebank.prometheusEndpoint" -}}
{{- if .Values.observability.prometheus.enabled -}}
http://prometheus:9090
{{- else -}}
{{ required "observability.prometheus.external.endpoint is required when prometheus.enabled=false" .Values.observability.prometheus.external.endpoint }}
{{- end -}}
{{- end }}

{{/* ===========================================
    Service Template
    =========================================== */}}

{{/*
Standard Deployment template for microservices
Usage: {{ include "litebank.deployment" (dict "name" "user-service" "config" .Values.services.userService "root" . "needsDb" true "needsKafka" false) }}
*/}}
{{- define "litebank.deployment" -}}
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ .name }}
  labels:
    {{- include "litebank.labels" .root | nindent 4 }}
    app.kubernetes.io/name: {{ .name }}
spec:
  replicas: {{ .config.replicas | default 1 }}
  selector:
    matchLabels:
      app.kubernetes.io/name: {{ .name }}
      app.kubernetes.io/instance: {{ .root.Release.Name }}
  template:
    metadata:
      labels:
        app.kubernetes.io/name: {{ .name }}
        app.kubernetes.io/instance: {{ .root.Release.Name }}
    spec:
      {{- with .root.Values.global.imagePullSecrets }}
      imagePullSecrets:
        {{- toYaml . | nindent 8 }}
      {{- end }}
      containers:
        - name: {{ .name }}
          image: "{{ .root.Values.global.imageRegistry }}{{ .config.image }}"
          imagePullPolicy: {{ .root.Values.services.common.imagePullPolicy }}
          ports:
            - name: http
              containerPort: {{ .config.port }}
              protocol: TCP
          env:
            - name: SERVER_PORT
              value: {{ .config.port | quote }}
            - name: ENVIRONMENT
              value: "kubernetes"
            - name: OTEL_SERVICE_NAME
              value: {{ .name }}
            - name: OTEL_RESOURCE_ATTRIBUTES
              value: "service.namespace={{ .root.Release.Namespace }}"
            - name: OTEL_EXPORTER_OTLP_ENDPOINT
              value: {{ include "litebank.otelEndpoint" .root }}
            {{- if .needsDb }}
            - name: DB_HOST
              value: {{ include "litebank.postgresHost" .root }}
            - name: DB_PORT
              value: {{ include "litebank.postgresPort" .root | quote }}
            - name: DB_NAME
              value: {{ .root.Values.infrastructure.postgres.database }}
            - name: DB_USER
              value: {{ .root.Values.infrastructure.postgres.user }}
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: litebank-secrets
                  key: db-password
            {{- end }}
            {{- if .needsKafka }}
            - name: KAFKA_BOOTSTRAP_SERVERS
              value: {{ include "litebank.kafkaServers" .root }}
            {{- end }}
            {{- if .needsJwt }}
            - name: JWT_SECRET
              valueFrom:
                secretKeyRef:
                  name: litebank-secrets
                  key: jwt-secret
            - name: JWT_EXPIRATION
              valueFrom:
                secretKeyRef:
                  name: litebank-secrets
                  key: jwt-expiration
            {{- end }}
            {{- if .extraEnv }}
            {{- toYaml .extraEnv | nindent 12 }}
            {{- end }}
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: http
            initialDelaySeconds: 90
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: http
            initialDelaySeconds: 30
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3
          resources:
            {{- toYaml .root.Values.services.common.resources | nindent 12 }}
{{- end }}

{{/*
Standard Service template
*/}}
{{- define "litebank.service" -}}
apiVersion: v1
kind: Service
metadata:
  name: {{ .name }}
  labels:
    {{- include "litebank.labels" .root | nindent 4 }}
    app.kubernetes.io/name: {{ .name }}
spec:
  type: ClusterIP
  ports:
    - port: {{ .config.port }}
      targetPort: http
      protocol: TCP
      name: http
  selector:
    app.kubernetes.io/name: {{ .name }}
    app.kubernetes.io/instance: {{ .root.Release.Name }}
{{- end }}
