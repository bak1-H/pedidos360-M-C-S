#!/bin/bash
set -euxo pipefail

dnf install -y java-21-amazon-corretto-headless awscli nginx

mkdir -p /opt/pedidos360
cd /opt/pedidos360

for svc in usuarios-service pedidos-service envios-service bff; do
  aws s3 cp "s3://__BUCKET__/${svc}.jar" "/opt/pedidos360/${svc}.jar"
done

cat > /opt/pedidos360/backend.env <<'ENVEOF'
SPRING_PROFILES_ACTIVE=aws
JAVA_TOOL_OPTIONS=-Xmx256m
DB_URL=jdbc:postgresql://__DB_HOST__:5432/pedidos360
DB_USERNAME=__DB_USERNAME__
DB_PASSWORD=__DB_PASSWORD__
INTERNAL_TOKEN=__INTERNAL_TOKEN__
AZURE_TENANT_ID=__AZURE_TENANT_ID__
AZURE_API_CLIENT_ID=__AZURE_API_CLIENT_ID__
USUARIOS_SERVICE_URL=http://localhost:8081
PEDIDOS_SERVICE_URL=http://localhost:8082
ENVIOS_SERVICE_URL=http://localhost:8083
CORS_ORIGINS=__API_GATEWAY_URL__,http://localhost:4200
ENVEOF
chmod 600 /opt/pedidos360/backend.env

crear_unidad() {
  local nombre=$1
  cat > "/etc/systemd/system/${nombre}.service" <<UNITEOF
[Unit]
Description=Pedidos360 ${nombre}
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
EnvironmentFile=/opt/pedidos360/backend.env
ExecStart=/usr/bin/java -jar /opt/pedidos360/${nombre}.jar
Restart=always
RestartSec=10
User=root

[Install]
WantedBy=multi-user.target
UNITEOF
}

for svc in usuarios-service pedidos-service envios-service bff; do
  crear_unidad "$svc"
done

systemctl daemon-reload
for svc in usuarios-service pedidos-service envios-service; do
  systemctl enable --now "$svc"
done

sleep 45
systemctl enable --now bff

aws s3 cp "s3://__BUCKET__/frontend.tar.gz" /tmp/frontend.tar.gz
tar -xzf /tmp/frontend.tar.gz -C /usr/share/nginx/html
find /usr/share/nginx/html -maxdepth 1 -name 'chunk-*.js' -mtime +7 -delete
aws s3 cp "s3://__BUCKET__/pedidos360-nginx.conf" /etc/nginx/conf.d/pedidos360.conf
nginx -t
systemctl enable --now nginx
systemctl restart nginx
