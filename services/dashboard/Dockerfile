# Build stage
FROM node:22-alpine AS builder

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy source code
COPY . .

# Build the application
RUN npm run build

# Production stage
FROM nginx:alpine

# Copy nginx config template (not the final config)
COPY nginx.conf.template /etc/nginx/templates/default.conf.template

# Copy built files from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Default environment variables
ENV API_GATEWAY_URL=http://api-gateway:8080
ENV NOTIFICATION_SERVICE_URL=http://notification-service:8089

# Expose port
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:80/ || exit 1

# nginx:alpine image automatically runs envsubst on /etc/nginx/templates/*.template
# and outputs to /etc/nginx/conf.d/
CMD ["nginx", "-g", "daemon off;"]
