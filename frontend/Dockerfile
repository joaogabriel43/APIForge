# Stage 1: Build the Angular standalone app
FROM node:20-alpine AS builder
WORKDIR /app

# Install dependencies first utilizing Docker layer cache
COPY package*.json ./
RUN npm ci

# Copy source and build Angular app in production-like compile
COPY . .
RUN npm run build

# Stage 2: Serve the app via lightweight Nginx
FROM nginx:1.25-alpine
COPY --from=builder /app/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
