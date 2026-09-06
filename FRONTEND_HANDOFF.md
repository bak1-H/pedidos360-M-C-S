# Contexto para el frontend Angular — Pedidos360

Este documento es para quien (persona o IA asistente) va a implementar `frontend/` en este monorepo. Resume qué existe hoy en el backend, contra qué endpoints tiene que integrar Angular, y qué falta por hacer del lado frontend. Es un complemento de `SDD_Pedidos360.md` (fuente de verdad del diseño) — ante cualquier duda de contrato de API, ese documento y el código real del `bff` mandan sobre este resumen.

## 1. Qué existe hoy en el repo

Monorepo Maven multi-módulo:

```
pedidos360-backend/
├── bff/                 completo: seguridad JWT + orquestación hacia los 3 microservicios
├── usuarios-service/    completo
├── pedidos-service/     completo
├── envios-service/      completo
└── frontend/            solo esqueleto de Angular (ng new), SIN MSAL, SIN vistas, SIN lógica
```

Los 4 módulos backend compilan y tienen tests pasando. `frontend/` es literalmente el scaffold por defecto de `ng new` más esta estructura de carpetas vacía (creada de antemano, sección 5.2 del SDD):

```
frontend/src/app/
├── core/auth/          vacío — acá va la config de MSAL, guards, interceptor
├── core/services/      vacío — acá va el servicio HTTP hacia el BFF
├── features/pedidos/   vacío
├── features/envios/    vacío
├── features/admin/     vacío
└── shared/             vacío
```

**Regla de oro del SDD: Angular NUNCA le habla directo a los microservicios. Todo pasa por el BFF, que corre en `http://localhost:8080` en desarrollo.** El `protectedResourceMap` de MSAL tiene que apuntar solo a esa URL base.

## 2. Qué falta del lado frontend (nada de esto existe todavía)

Del checklist de la sección 5.1 del SDD:

- [ ] Instalar `@azure/msal-angular` y `@azure/msal-browser`
- [ ] `MsalModule.forRoot()` con `clientId`, `authority` (`https://login.microsoftonline.com/{tenantId}`), `redirectUri`
- [ ] `MsalInterceptorConfiguration.protectedResourceMap` apuntando **solo** a `http://localhost:8080/bff/*`
- [ ] `MsalInterceptor` registrado en `HTTP_INTERCEPTORS` (adjunta el Bearer token automático)
- [ ] `MsalGuard` en las rutas protegidas
- [ ] Login/logout: `loginRedirect`/`loginPopup`, `logoutRedirect`
- [ ] Leer roles desde `account.idTokenClaims.roles` para mostrar/ocultar UI (ADMIN, REPARTIDOR, CLIENTE)
- [ ] Manejo del estado de carga durante el redirect de MSAL (evitar pantalla en blanco)
- [ ] Las vistas funcionales: login, listar/crear pedidos, tracking de un pedido, panel admin, panel repartidor

Errores típicos a evitar (SDD 5.3): apuntar el interceptor a los microservicios en vez de al BFF, usar `idToken` en vez de `accessToken` para llamar al BFF.

**El App Registration en Azure AD (clientId, tenantId, App Roles `ADMIN`/`REPARTIDOR`/`CLIENTE`) todavía no está creado.** Sin eso no hay con qué loguearse de verdad — es un bloqueante compartido con el equipo de backend, no algo que el frontend resuelva solo.

## 3. CORS ya configurado

El BFF permite el origen `http://localhost:4200` por defecto (`CorsConfig.java`, variable de entorno `CORS_ORIGINS`), con métodos `GET/POST/PATCH/PUT/DELETE/OPTIONS`, headers `Authorization`/`Content-Type`, y `credentials: true`. Si Angular corre en otro puerto, avisar para agregar el origen.

## 4. Contrato completo de la API del BFF

Base URL (dev): `http://localhost:8080`

Todo endpoint bajo `/bff/**` exige `Authorization: Bearer <access_token>` salvo `/actuator/health`. Sin token → `401`. Con token pero rol incorrecto → `403`.

### 4.1 Perfil propio

**`GET /bff/me`** — cualquier usuario autenticado. Se llama una vez después del login; si es la primera vez que el usuario entra, el backend le crea el perfil solo.

Respuesta (`PerfilResponse`):
```json
{
  "id": "uuid",
  "azureAdObjectId": "string",
  "nombre": "string",
  "email": "string",
  "rol": "CLIENTE",
  "roles": ["CLIENTE"],
  "recienCreado": true
}
```

### 4.2 Usuarios (solo ADMIN)

**`GET /bff/usuarios`** → `UsuarioDto[]`:
```json
[{ "id": "uuid", "azureAdObjectId": "string", "nombre": "string", "email": "string", "rol": "ADMIN", "fechaCreacion": "2026-01-01T10:00:00" }]
```

### 4.3 Pedidos

**`POST /bff/pedidos`** — rol `CLIENTE`. El `clienteId` NO se manda desde el front: el BFF lo resuelve del token.

Body (`CrearPedidoRequest`):
```json
{
  "direccionOrigen": "string",
  "direccionDestino": "string",
  "detalles": [{ "descripcion": "string", "cantidad": 1, "valorDeclarado": 15000.0 }],
  "paquete": { "pesoKg": 2.5, "alturaCm": 10, "anchoCm": 10, "largoCm": 10, "tipo": "CAJA" }
}
```
`tipo` es uno de: `DOCUMENTO`, `CAJA`, `FRAGIL`. Devuelve `201` + `PedidoResponseDto` (ver abajo).

**`GET /bff/pedidos/mios`** — rol `CLIENTE`. Devuelve `PedidoResponseDto[]` del usuario logueado.

**`GET /bff/pedidos`** — rol `ADMIN`. Devuelve todos los `PedidoResponseDto[]`.

**`GET /bff/pedidos/{id}`** — dueño del pedido o `ADMIN` (si no sos ninguno de los dos, `403`; si no existe, `404`). Devuelve `PedidoConTrackingResponse`:
```json
{
  "pedido": {
    "id": "uuid", "clienteId": "uuid", "estado": "CREADO",
    "direccionOrigen": "string", "direccionDestino": "string",
    "fechaCreacion": "2026-01-01T10:00:00",
    "detalles": [{ "descripcion": "string", "cantidad": 1, "valorDeclarado": 15000.0 }],
    "paquete": { "pesoKg": 2.5, "alturaCm": 10, "anchoCm": 10, "largoCm": 10, "tipo": "CAJA" }
  },
  "envio": null
}
```
`estado` es uno de: `CREADO`, `DESPACHADO`, `ENTREGADO`, `CANCELADO`. `envio` viene `null` hasta que un ADMIN cree el envío asociado (ver 4.4); cuando existe, trae el mismo shape que `EnvioResponseDto`.

### 4.4 Envíos

**`POST /bff/envios`** — rol `ADMIN`. Crea el envío y asigna repartidor.

Body (`EnvioRequestDto`):
```json
{ "pedidoId": "uuid", "repartidorId": "uuid", "fechaEntregaEstimada": "2026-01-05T18:00:00" }
```
`fechaEntregaEstimada` es opcional. Devuelve `201` + `EnvioResponseDto`.

**`PATCH /bff/envios/{id}/estado`** — el repartidor dueño del envío, o `ADMIN`. Si sos repartidor y el envío no es tuyo → `403`.

Body (`CambioEstadoEnvioDto`):
```json
{ "estadoEnvio": "EN_TRANSITO", "descripcionEvento": "Salió a reparto" }
```
`estadoEnvio` es uno de: `PENDIENTE`, `EN_TRANSITO`, `ENTREGADO`, `FALLIDO`. Devuelve `200` + `EnvioResponseDto`.

**`GET /bff/envios/mios`** — rol `REPARTIDOR`. Devuelve `EnvioResponseDto[]` asignados al repartidor logueado.

`EnvioResponseDto`:
```json
{ "id": "uuid", "pedidoId": "uuid", "repartidorId": "uuid", "estadoEnvio": "PENDIENTE", "fechaAsignacion": "2026-01-01T10:00:00", "fechaEntregaEstimada": null }
```

### 4.5 Formato de error (siempre JSON, nunca HTML)

```json
{
  "status": 403,
  "codigo": "SIN_PERMISO",
  "mensaje": "El pedido no pertenece al usuario autenticado",
  "ruta": "/bff/pedidos/...",
  "timestamp": "2026-01-01T10:00:00Z"
}
```
Códigos posibles: `NO_AUTENTICADO` (401), `SIN_PERMISO` (403), `NO_ENCONTRADO` (404), `ERROR_MICROSERVICIO` (502, algo falló en un microservicio interno), `MICROSERVICIO_INALCANZABLE` (504).

## 5. Roles y qué UI corresponde a cada uno

| Rol | Ve/Hace |
|---|---|
| `CLIENTE` | Crea pedidos (`POST /bff/pedidos`), ve los suyos (`/bff/pedidos/mios`), ve el detalle+tracking de uno propio |
| `REPARTIDOR` | Ve sus envíos asignados (`/bff/envios/mios`), actualiza estado (`PATCH /bff/envios/{id}/estado`) |
| `ADMIN` | Todo lo anterior sin restricción de dueño, más `/bff/usuarios`, `/bff/pedidos` (todos), `POST /bff/envios` (asignar repartidor) |

El rol viene en `account.idTokenClaims.roles` después del login MSAL — es un array porque un usuario podría tener más de uno, aunque en la práctica cada usuario de prueba va a tener uno solo.

## 6. Puertos en desarrollo

| Servicio | Puerto |
|---|---|
| bff | 8080 |
| usuarios-service | 8081 |
| pedidos-service | 8082 |
| envios-service | 8083 |
| Angular (`ng serve`) | 4200 |

Angular solo necesita saber del **8080**. Los demás son internos y no deberían ser alcanzables desde el navegador.
