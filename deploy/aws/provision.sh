#!/usr/bin/env bash
set -euo pipefail

export AWS_DEFAULT_REGION="${AWS_DEFAULT_REGION:-us-west-2}"
AWS="${AWS:-aws}"
PROJECT=pedidos360
INSTANCE_TYPE="${INSTANCE_TYPE:-t3.large}"
AZURE_TENANT_ID="${AZURE_TENANT_ID:-a0937f8c-6713-4912-8198-29b1db9da9a5}"
AZURE_API_CLIENT_ID="${AZURE_API_CLIENT_ID:-598d6a64-0770-4213-bb07-34601e54499d}"
COMPOSE_VERSION=v2.39.2
BUILDX_VERSION=v0.26.1

tags() { echo "ResourceType=$1,Tags=[{Key=Project,Value=$PROJECT},{Key=Name,Value=$PROJECT}]"; }
none() { [ -z "$1" ] || [ "$1" = "None" ]; }

ACCOUNT_ID=$($AWS sts get-caller-identity --query Account --output text)
BUCKET="${PROJECT}-deploy-${ACCOUNT_ID}-uw2"

if ! $AWS s3api head-bucket --bucket "$BUCKET" 2>/dev/null; then
  $AWS s3api create-bucket --bucket "$BUCKET" \
    --create-bucket-configuration LocationConstraint="$AWS_DEFAULT_REGION" >/dev/null
fi
echo "Bucket: $BUCKET"

VPC_ID=$($AWS ec2 describe-vpcs --filters Name=is-default,Values=true --query 'Vpcs[0].VpcId' --output text)
IGW_ID=$($AWS ec2 describe-internet-gateways --filters "Name=attachment.vpc-id,Values=$VPC_ID" \
  --query 'InternetGateways[0].InternetGatewayId' --output text)
MAIN_RT=$($AWS ec2 describe-route-tables --filters "Name=vpc-id,Values=$VPC_ID" Name=association.main,Values=true \
  --query 'RouteTables[0].RouteTableId' --output text)
if none "$($AWS ec2 describe-route-tables --route-table-ids "$MAIN_RT" \
  --query "RouteTables[0].Routes[?DestinationCidrBlock=='0.0.0.0/0'].GatewayId | [0]" --output text)"; then
  $AWS ec2 create-route --route-table-id "$MAIN_RT" --destination-cidr-block 0.0.0.0/0 --gateway-id "$IGW_ID" >/dev/null
fi
SG_ID=$($AWS ec2 describe-security-groups --filters "Name=group-name,Values=${PROJECT}-sg" "Name=vpc-id,Values=$VPC_ID" \
  --query 'SecurityGroups[0].GroupId' --output text)
if none "$SG_ID"; then
  SG_ID=$($AWS ec2 create-security-group --group-name "${PROJECT}-sg" --description "Pedidos360 web" \
    --vpc-id "$VPC_ID" --tag-specifications "$(tags security-group)" --query GroupId --output text)
  for port in 80 443; do
    $AWS ec2 authorize-security-group-ingress --group-id "$SG_ID" --protocol tcp --port "$port" --cidr 0.0.0.0/0 >/dev/null
  done
fi
echo "Security group: $SG_ID"

ALLOC_ID=$($AWS ec2 describe-addresses --filters "Name=tag:Project,Values=$PROJECT" --query 'Addresses[0].AllocationId' --output text)
if none "$ALLOC_ID"; then
  ALLOC_ID=$($AWS ec2 allocate-address --domain vpc --tag-specifications "$(tags elastic-ip)" --query AllocationId --output text)
fi
SITE_IP=$($AWS ec2 describe-addresses --allocation-ids "$ALLOC_ID" --query 'Addresses[0].PublicIp' --output text)
SITE_HOST="${SITE_IP//./-}.nip.io"
echo "Sitio: https://$SITE_HOST"

INSTANCE_ID=$($AWS ec2 describe-instances \
  --filters "Name=tag:Project,Values=$PROJECT" "Name=instance-state-name,Values=pending,running,stopping,stopped" \
  --query 'Reservations[0].Instances[0].InstanceId' --output text)
if none "$INSTANCE_ID"; then
  USER_DATA=$(mktemp)
  cat > "$USER_DATA" <<EOF
#!/bin/bash
set -e
dnf install -y docker git
systemctl enable --now docker
mkdir -p /usr/local/lib/docker/cli-plugins
curl -fsSL https://github.com/docker/compose/releases/download/${COMPOSE_VERSION}/docker-compose-linux-x86_64 \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
curl -fsSL https://github.com/docker/buildx/releases/download/${BUILDX_VERSION}/buildx-${BUILDX_VERSION}.linux-amd64 \
  -o /usr/local/lib/docker/cli-plugins/docker-buildx
chmod +x /usr/local/lib/docker/cli-plugins/*
if [ ! -f /swapfile ]; then
  fallocate -l 4G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
  echo '/swapfile swap swap defaults 0 0' >> /etc/fstab
fi
mkdir -p /opt/pedidos360
if [ ! -f /opt/pedidos360/.env ]; then
  cat > /opt/pedidos360/.env <<ENV
POSTGRES_USER=pedidos360
POSTGRES_PASSWORD=\$(openssl rand -hex 16)
RABBITMQ_USER=pedidos360
RABBITMQ_PASSWORD=\$(openssl rand -hex 16)
INTERNAL_TOKEN=\$(openssl rand -hex 24)
AZURE_TENANT_ID=${AZURE_TENANT_ID}
AZURE_API_CLIENT_ID=${AZURE_API_CLIENT_ID}
SITE_HOST=${SITE_HOST}
ENV
  chmod 600 /opt/pedidos360/.env
fi
EOF
  INSTANCE_ID=$($AWS ec2 run-instances \
    --image-id resolve:ssm:/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64 \
    --instance-type "$INSTANCE_TYPE" \
    --iam-instance-profile Name=LabInstanceProfile \
    --security-group-ids "$SG_ID" \
    --block-device-mappings 'DeviceName=/dev/xvda,Ebs={VolumeSize=30,VolumeType=gp3}' \
    --user-data "file://$(cygpath -w "$USER_DATA" 2>/dev/null || echo "$USER_DATA")" \
    --tag-specifications "$(tags instance)" "$(tags volume)" \
    --query 'Instances[0].InstanceId' --output text)
  rm -f "$USER_DATA"
fi
$AWS ec2 wait instance-running --instance-ids "$INSTANCE_ID"
$AWS ec2 associate-address --instance-id "$INSTANCE_ID" --allocation-id "$ALLOC_ID" >/dev/null
echo "Instancia: $INSTANCE_ID"

API_ID=$($AWS apigatewayv2 get-apis --query "Items[?Name=='${PROJECT}-api'].ApiId | [0]" --output text)
if none "$API_ID"; then
  API_ID=$($AWS apigatewayv2 create-api --name "${PROJECT}-api" --protocol-type HTTP \
    --tags "Project=$PROJECT" --query ApiId --output text)
  AUTH_ID=$($AWS apigatewayv2 create-authorizer --api-id "$API_ID" --name azure-entra-jwt \
    --authorizer-type JWT --identity-source '$request.header.Authorization' \
    --jwt-configuration "Audience=$AZURE_API_CLIENT_ID,Issuer=https://login.microsoftonline.com/$AZURE_TENANT_ID/v2.0" \
    --query AuthorizerId --output text)
  INTEGRATION_ID=$($AWS apigatewayv2 create-integration --api-id "$API_ID" \
    --integration-type HTTP_PROXY --integration-method ANY \
    --integration-uri "https://${SITE_HOST}/bff/{proxy}" --payload-format-version 1.0 \
    --query IntegrationId --output text)
  $AWS apigatewayv2 create-route --api-id "$API_ID" --route-key 'ANY /bff/{proxy+}' \
    --authorization-type JWT --authorizer-id "$AUTH_ID" --target "integrations/$INTEGRATION_ID" >/dev/null
  $AWS apigatewayv2 create-route --api-id "$API_ID" --route-key 'OPTIONS /bff/{proxy+}' \
    --target "integrations/$INTEGRATION_ID" >/dev/null
  $AWS apigatewayv2 create-stage --api-id "$API_ID" --stage-name '$default' --auto-deploy >/dev/null
fi
echo "API Manager: $($AWS apigatewayv2 get-api --api-id "$API_ID" --query ApiEndpoint --output text)"
