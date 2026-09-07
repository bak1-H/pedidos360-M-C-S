#!/usr/bin/env bash
set -uo pipefail

GATEWAY="${GATEWAY:-https://4m27pk8l7a.execute-api.us-east-1.amazonaws.com}"
TOKEN_ADMIN="${TOKEN_ADMIN:-}"
TOKEN_CLIENTE="${TOKEN_CLIENTE:-}"
TOKEN_REPARTIDOR="${TOKEN_REPARTIDOR:-}"
MAX_LINEAS="${MAX_LINEAS:-14}"

uso() {
  cat <<'AYUDA'
Recorre las rutas del API Gateway y muestra el codigo HTTP y el cuerpo JSON.

Uso:
  ./evidencia.sh                 Solo las pruebas sin token (401 esperado)
  TOKEN_ADMIN=... ./evidencia.sh Agrega las rutas de ADMIN

  TOKEN_ADMIN=... TOKEN_CLIENTE=... TOKEN_REPARTIDOR=... ./evidencia.sh
                                 Evidencia completa, incluido el flujo
                                 POST pedido -> POST envio -> PATCH estado

Variables opcionales:
  GATEWAY      URL base del API Gateway
  MAX_LINEAS   Lineas de JSON a mostrar por respuesta (por defecto 14)

Para obtener un token: inicia sesion en el frontend con el usuario del rol que
necesitas, abre DevTools, entra a Network y copia el encabezado Authorization
de cualquier peticion a /bff/ sin el prefijo "Bearer ".
AYUDA
}

if [ "${1:-}" = "--help" ] || [ "${1:-}" = "-h" ]; then
  uso
  exit 0
fi

if [ -t 1 ]; then
  ROJO=$'\033[1;31m'
  VERDE=$'\033[1;32m'
  AZUL=$'\033[1;34m'
  GRIS=$'\033[0;90m'
  AMARILLO=$'\033[1;33m'
  FIN=$'\033[0m'
else
  ROJO=''; VERDE=''; AZUL=''; GRIS=''; AMARILLO=''; FIN=''
fi

if command -v jq >/dev/null 2>&1; then
  FORMATEADOR=jq
elif command -v python3 >/dev/null 2>&1; then
  FORMATEADOR=python3
else
  FORMATEADOR=ninguno
fi

OK=0
FALLOS=0
ULTIMO_CUERPO=''

titulo() {
  printf '\n%s%s%s\n' "$AZUL" "$1" "$FIN"
}

formatear_json() {
  case "$FORMATEADOR" in
    jq)      jq . 2>/dev/null || cat ;;
    python3) python3 -m json.tool 2>/dev/null || cat ;;
    *)       cat ;;
  esac
}

campo() {
  local cuerpo=$1 clave=$2
  case "$FORMATEADOR" in
    jq)
      printf '%s' "$cuerpo" | jq -r --arg k "$clave" '.[$k] // empty' 2>/dev/null
      ;;
    python3)
      printf '%s' "$cuerpo" | python3 -c 'import json,sys
try:
    d = json.load(sys.stdin)
except Exception:
    sys.exit()
v = d.get(sys.argv[1]) if isinstance(d, dict) else None
if v is not None:
    print(v)' "$clave" 2>/dev/null
      ;;
  esac
}

id_por_rol() {
  local cuerpo=$1 rol=$2
  case "$FORMATEADOR" in
    jq)
      printf '%s' "$cuerpo" | jq -r --arg r "$rol" \
        'map(select(.rol == $r)) | first | .id // empty' 2>/dev/null
      ;;
    python3)
      printf '%s' "$cuerpo" | python3 -c 'import json,sys
try:
    d = json.load(sys.stdin)
except Exception:
    sys.exit()
for u in d if isinstance(d, list) else []:
    if isinstance(u, dict) and u.get("rol") == sys.argv[1] and u.get("id"):
        print(u["id"])
        break' "$rol" 2>/dev/null
      ;;
  esac
}

probar() {
  local metodo=$1 ruta=$2 token=$3 esperado=$4 nota=${5:-} envio=${6:-}
  local args=(-s -m 25 -w $'\n%{http_code}' -X "$metodo" "$GATEWAY$ruta")

  if [ -n "$envio" ]; then
    args+=(-H 'Content-Type: application/json' -d "$envio")
  fi
  if [ -n "$token" ]; then
    args+=(-H "Authorization: Bearer $token")
  fi

  local respuesta codigo cuerpo color alerta
  respuesta=$(curl "${args[@]}")
  codigo=${respuesta##*$'\n'}
  cuerpo=${respuesta%$'\n'*}

  if [ "$codigo" = "$esperado" ]; then
    color=$VERDE
    alerta=''
    OK=$((OK + 1))
  else
    color=$ROJO
    alerta="${ROJO}<- se esperaba ${esperado}${FIN}"
    FALLOS=$((FALLOS + 1))
  fi

  printf '  %s%-3s%s %-6s %-38s %s%s%s %s\n' \
    "$color" "$codigo" "$FIN" "$metodo" "$ruta" "$GRIS" "$nota" "$FIN" "$alerta"

  if [ -n "$cuerpo" ]; then
    printf '%s' "$cuerpo" | formatear_json | head -n "$MAX_LINEAS" \
      | while IFS= read -r linea; do printf '      %s%s%s\n' "$GRIS" "$linea" "$FIN"; done
  fi

  ULTIMO_CUERPO=$cuerpo
}

resumen() {
  titulo 'RESUMEN'
  printf '  %sCoinciden: %d%s   %sNo coinciden: %d%s\n\n' \
    "$VERDE" "$OK" "$FIN" "$ROJO" "$FALLOS" "$FIN"
}

printf '%sGateway:%s %s\n' "$AMARILLO" "$FIN" "$GATEWAY"
if [ "$FORMATEADOR" = ninguno ]; then
  printf '%sSin jq ni python3: el JSON se muestra sin formato y el flujo encadenado se omite.%s\n' \
    "$AMARILLO" "$FIN"
fi

titulo 'FRONTEND (publico, sin autenticacion)'
probar GET / '' 200 'la SPA se sirve sin token'
probar GET /pedidos '' 200 'ruta de Angular resuelta por nginx'

titulo 'BACKEND SIN TOKEN (el gateway corta antes de llegar al computo)'
probar GET   /bff/me                                                 '' 401 ''
probar GET   /bff/usuarios                                           '' 401 ''
probar GET   /bff/pedidos                                            '' 401 ''
probar GET   /bff/pedidos/mios                                       '' 401 ''
probar GET   /bff/pedidos/00000000-0000-0000-0000-000000000000       '' 401 ''
probar POST  /bff/pedidos                                            '' 401 '' '{}'
probar GET   /bff/envios/mios                                        '' 401 ''
probar POST  /bff/envios                                             '' 401 '' '{}'
probar PATCH /bff/envios/00000000-0000-0000-0000-000000000000/estado '' 401 '' '{}'

titulo 'BACKEND CON TOKEN INVALIDO'
probar GET /bff/me 'token-invalido' 401 'la firma no valida contra Entra ID'

if [ -z "$TOKEN_ADMIN$TOKEN_CLIENTE$TOKEN_REPARTIDOR" ]; then
  titulo 'SIN TOKENS: se omiten las pruebas 200, 201 y 403'
  printf '  Exporta TOKEN_ADMIN, TOKEN_CLIENTE y TOKEN_REPARTIDOR para la evidencia completa.\n'
  printf '  Ejecuta ./evidencia.sh --help para ver como obtenerlos.\n'
  resumen
  exit $((FALLOS > 0))
fi

REPARTIDOR_ID=''
PEDIDO_ID=''
ENVIO_ID=''

if [ -n "$TOKEN_ADMIN" ]; then
  titulo 'ROL ADMIN'
  probar GET /bff/me           "$TOKEN_ADMIN" 200 'perfil resuelto desde el claim roles'
  probar GET /bff/usuarios     "$TOKEN_ADMIN" 200 'listado completo, exclusivo de ADMIN'
  REPARTIDOR_ID=$(id_por_rol "$ULTIMO_CUERPO" REPARTIDOR)
  probar GET /bff/pedidos      "$TOKEN_ADMIN" 200 'todos los pedidos del sistema'
  probar GET /bff/pedidos/mios "$TOKEN_ADMIN" 403 'token valido, pero ADMIN no es CLIENTE'
fi

if [ -n "$TOKEN_CLIENTE" ]; then
  titulo 'ROL CLIENTE'
  probar GET /bff/pedidos "$TOKEN_CLIENTE" 403 'listar todo es exclusivo de ADMIN'
  probar POST /bff/pedidos "$TOKEN_CLIENTE" 201 'el cliente se resuelve desde el token' '{
  "direccionOrigen": "Nunoa 1234, Santiago",
  "direccionDestino": "La Florida 5678, Santiago",
  "detalles": [{"descripcion": "Notebook", "cantidad": 1, "valorDeclarado": 850000}],
  "paquete": {"pesoKg": 2.5, "alturaCm": 10, "anchoCm": 35, "largoCm": 25, "tipo": "FRAGIL"}
}'
  PEDIDO_ID=$(campo "$ULTIMO_CUERPO" id)
  probar GET /bff/pedidos/mios "$TOKEN_CLIENTE" 200 'solo los pedidos del cliente autenticado'
  if [ -n "$PEDIDO_ID" ]; then
    probar GET "/bff/pedidos/$PEDIDO_ID" "$TOKEN_CLIENTE" 200 'el dueno accede a su propio pedido'
  fi
fi

if [ -n "$TOKEN_ADMIN" ] && [ -n "$PEDIDO_ID" ] && [ -n "$REPARTIDOR_ID" ]; then
  titulo 'ASIGNACION DEL ENVIO (ADMIN)'
  probar POST /bff/envios "$TOKEN_ADMIN" 201 "pedido $PEDIDO_ID al repartidor $REPARTIDOR_ID" "{
  \"pedidoId\": \"$PEDIDO_ID\",
  \"repartidorId\": \"$REPARTIDOR_ID\"
}"
  ENVIO_ID=$(campo "$ULTIMO_CUERPO" id)
fi

if [ -n "$TOKEN_REPARTIDOR" ]; then
  titulo 'ROL REPARTIDOR'
  probar GET /bff/envios/mios "$TOKEN_REPARTIDOR" 200 'solo los envios asignados a el'
  probar GET /bff/usuarios    "$TOKEN_REPARTIDOR" 403 'el listado de usuarios es de ADMIN'
  if [ -n "$ENVIO_ID" ]; then
    probar PATCH "/bff/envios/$ENVIO_ID/estado" "$TOKEN_REPARTIDOR" 200 'avanza el estado de su envio' '{
  "estadoEnvio": "EN_TRANSITO",
  "descripcionEvento": "Retirado desde el origen"
}'
  fi
fi

resumen
exit $((FALLOS > 0))
