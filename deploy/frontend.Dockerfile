FROM node:22-alpine AS build
ARG SITE_HOST
ARG API_BASE_URL
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
COPY deploy/set-site-host.js /tmp/set-site-host.js
RUN SITE_HOST="${SITE_HOST}" API_BASE_URL="${API_BASE_URL}" node /tmp/set-site-host.js
RUN NODE_OPTIONS=--max-old-space-size=1536 npm run build

FROM nginx:alpine
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
