#!/usr/bin/env bash
set -euo pipefail

export AWS_DEFAULT_REGION="${AWS_DEFAULT_REGION:-us-west-2}"
AWS="${AWS:-aws}"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

ACCOUNT_ID=$($AWS sts get-caller-identity --query Account --output text)
BUCKET="pedidos360-deploy-${ACCOUNT_ID}-uw2"
INSTANCE_ID=$($AWS ec2 describe-instances \
  --filters "Name=tag:Project,Values=pedidos360" "Name=instance-state-name,Values=pending,running,stopping,stopped" \
  --query 'Reservations[0].Instances[0].InstanceId' --output text)
API_URL=$($AWS apigatewayv2 get-apis --query "Items[?Name=='pedidos360-api'].ApiEndpoint | [0]" --output text)

if [ "$($AWS ec2 describe-instances --instance-ids "$INSTANCE_ID" --query 'Reservations[0].Instances[0].State.Name' --output text)" = "stopped" ]; then
  $AWS ec2 start-instances --instance-ids "$INSTANCE_ID" >/dev/null
fi
$AWS ec2 wait instance-running --instance-ids "$INSTANCE_ID"
until [ "$($AWS ssm describe-instance-information --filters "Key=InstanceIds,Values=$INSTANCE_ID" \
  --query 'InstanceInformationList[0].PingStatus' --output text)" = "Online" ]; do sleep 10; done

PACKAGE="$(mktemp -d)/pedidos360.tar.gz"
tar -czf "$PACKAGE" -C "$ROOT" \
  --exclude=.git --exclude=node_modules --exclude=target --exclude=dist \
  --exclude=.angular --exclude=.atl --exclude='*.log' --exclude='.env*' .
$AWS s3 cp "$(cygpath -w "$PACKAGE" 2>/dev/null || echo "$PACKAGE")" "s3://${BUCKET}/pedidos360.tar.gz" --only-show-errors

REMOTE=$(cat <<EOF
set -e
cloud-init status --wait >/dev/null
export AWS_DEFAULT_REGION=$AWS_DEFAULT_REGION COMPOSE_PARALLEL_LIMIT=2
cd /opt/pedidos360
aws s3 cp s3://${BUCKET}/pedidos360.tar.gz /tmp/pedidos360.tar.gz --only-show-errors
find /opt/pedidos360 -mindepth 1 -maxdepth 1 ! -name .env -exec rm -rf {} +
tar -xzf /tmp/pedidos360.tar.gz -C /opt/pedidos360
grep -q '^RABBITMQ_ERLANG_COOKIE=' .env || echo "RABBITMQ_ERLANG_COOKIE=\$(openssl rand -hex 24)" >> .env
grep -q '^KAFKA_UI_USER=' .env || printf 'KAFKA_UI_USER=admin\nKAFKA_UI_PASSWORD=%s\n' "\$(openssl rand -hex 12)" >> .env
grep -v '^API_BASE_URL=' .env > .env.next
echo "API_BASE_URL=${API_URL}" >> .env.next
chmod 600 .env.next && mv .env.next .env
COMPOSE="docker compose --env-file .env -f deploy/docker-compose.prod.yml"
\$COMPOSE up -d --build --remove-orphans || { sleep 60; \$COMPOSE up -d --remove-orphans; }
\$COMPOSE restart caddy
\$COMPOSE ps --format 'table {{.Service}}\t{{.Status}}'
EOF
)
ENCODED=$(printf '%s' "$REMOTE" | base64 -w0)
COMMAND_ID=$($AWS ssm send-command --instance-ids "$INSTANCE_ID" \
  --document-name AWS-RunShellScript --timeout-seconds 3600 \
  --parameters "commands=[\"echo ${ENCODED} | base64 -d | bash 2>&1\"]" \
  --query Command.CommandId --output text)
echo "Comando SSM: $COMMAND_ID"
while :; do
  STATUS=$($AWS ssm get-command-invocation --command-id "$COMMAND_ID" --instance-id "$INSTANCE_ID" \
    --query Status --output text 2>/dev/null || echo Pending)
  case "$STATUS" in Success|Failed|Cancelled|TimedOut) break ;; esac
  sleep 15
done
$AWS ssm get-command-invocation --command-id "$COMMAND_ID" --instance-id "$INSTANCE_ID" \
  --query StandardOutputContent --output text | tail -n 40
test "$STATUS" = "Success"
