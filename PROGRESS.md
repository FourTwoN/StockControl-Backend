# Demeter AI 2.0 — Backend Progress

**Stack:** Java 25 + Quarkus 3.31.2 + Gradle 9.3.1 + PostgreSQL 17
**Package base:** `com.fortytwo.demeter.*`
**Modules:** 13 Gradle subprojects
**Total Java files:** 201

---

## Task Manager

### FASE 1: Scaffolding y Configuracion Base
- [x] Gradle setup (root build.gradle.kts, settings.gradle.kts, gradle.properties, wrapper)
- [x] 13 module build.gradle.kts files with dependency declarations
- [x] Common module (14 files): tenant resolver, RLS customizer, BaseEntity, auth, exceptions, DTOs
- [x] Database migrations: V1 baseline (30 tables), V2 tenant indexes, V3 RLS policies
- [x] application.properties (dev/staging/prod profiles, OIDC, Flyway, OpenAPI)
- [x] Docker: Dockerfile (multi-stage), docker-compose.yml (PostgreSQL 17 + pgAdmin)
- [x] Deploy: Cloud Run service YAML, Cloud Build CI/CD, Terraform (Cloud Run + Cloud SQL)
- [x] .gitignore, .dockerignore

### FASE 2: Core Modules — Datos Maestros
- [x] **demeter-productos** (23 files): Product, Category, Family, State, Size, SampleImage, DensityParameter
- [x] **demeter-ubicaciones** (30 files): Warehouse, StorageArea, StorageLocation, StorageBin, StorageBinType (soft delete)
- [x] **demeter-empaquetado** (24 files): PackagingCatalog, PackagingType, PackagingMaterial, PackagingColor
- [x] **demeter-usuarios** (8 files): User, UserRole, OIDC mapping

### FASE 3: Core Modules — Logica de Negocio
- [x] **demeter-inventario** (17 files): StockBatch, StockMovement, StockBatchMovement, BatchStatus, MovementType (ENTRADA/MUERTE/TRASPLANTE/VENTA/AJUSTE), JSONB custom_attributes
- [x] **demeter-ventas** (13 files): Sale, SaleItem, SaleStatus, SaleCompletionService (cross-module stock movements)
- [x] **demeter-costos** (7 files): Cost tracking with product/batch reference, cost types, date ranges
- [x] **demeter-precios** (13 files): PriceList with versioning (effectiveDate), PriceEntry, bulk add, activate/deactivate
- [x] **demeter-analytics** (11 files): 7 read-only analytics endpoints (stock summary, movements, inventory valuation, top products, location occupancy, dashboard, movement history)

### FASE 4: DLC Modules
- [x] **demeter-fotos** (23 files): PhotoProcessingSession, S3Image, Detection, Classification, Estimation, session status polling
- [x] **demeter-chatbot** (18 files): ChatSession, ChatMessage, ChatToolExecution, JSONB input/output, message roles

### FASE 5: Testing e Integracion
- [x] Test configuration (application.properties for test profile, quarkus-test-security dependency)
- [x] ProductControllerTest — 7 CRUD integration tests
- [x] MultiTenantIsolationTest — 3 tenant A/B data isolation tests (products, batches, sales)
- [x] StockBatchControllerTest — 10 batch CRUD + status transitions + validation tests
- [x] SaleFlowTest — 14 E2E: product → batch → sale → complete/cancel + state transitions
- [x] HealthCheckTest — 3 tests: /q/health/live, /q/health/ready, /q/health
- [x] OpenApiTest — 5 tests: /q/openapi, Swagger UI, paths verification

### FASE 6: Tenant Config — Persistencia y Endpoint para Frontend

Spec del frontend: el frontend necesita UN endpoint al iniciar para resolver la personalizacion del tenant.

#### Endpoint requerido

```
GET /api/v1/tenants/{tenantId}/config    (@PermitAll — se llama antes del login)
```

#### Response JSON esperado

```json
{
  "id": "go-bar",
  "name": "Go Bar",
  "industry": "COMERCIANTES",
  "theme": {
    "primary": "#FF6B35",
    "secondary": "#0F172A",
    "accent": "#E65100",
    "background": "#F8FAFC",
    "logoUrl": "https://cdn.demeter.app/go-bar/logo.png",
    "appName": "Go Bar Stock"
  },
  "enabledModules": ["inventario", "productos", "ventas", "ubicaciones", "fotos"],
  "settings": {
    "currency": "ARS",
    "timezone": "America/Argentina/Buenos_Aires"
  }
}
```

#### enabledModules — valores validos (strings exactos)

| Key | Tipo | Descripcion |
|-----|------|-------------|
| inventario | core | Lotes de stock, movimientos |
| productos | core | Catalogo de productos, categorias, familias |
| ventas | core | Flujo de ventas, recibos |
| costos | core | Entries de costos, valuacion |
| usuarios | core | Gestion de usuarios, RBAC |
| ubicaciones | core | Warehouses, locations, bins, mapa |
| empaquetado | core | Catalogo de packaging |
| precios | core | Listas de precios, upload CSV |
| analytics | core | Dashboard, KPIs, graficos |
| fotos | DLC | Procesamiento de fotos, galeria |
| chatbot | DLC | Chat AI con streaming |

No hay distincion mecanica core/DLC — todos se filtran igual. La distincion es semantica.

#### Flujo completo

```
1. Usuario abre gobar.demeter.app
2. Frontend resuelve tenant_id = "go-bar" (del subdominio)
3. Frontend llama GET /api/v1/tenants/go-bar/config
4. Backend devuelve TenantConfig con theme + enabledModules
5. Frontend aplica:
   ├── CSS variables en :root (4 colores del theme)
   ├── Logo/nombre en header
   ├── Sidebar: solo modulos en enabledModules
   ├── Rutas: solo modulos en enabledModules
   └── MobileNav: primeros 5 modulos habilitados
6. Cada request API lleva Authorization: Bearer <jwt> + X-Tenant-ID: go-bar
7. Backend filtra datos con RLS usando tenant_id
```

#### Tasks de implementacion

- [x] **V4 Flyway migration** — `V4__create_tenants_table.sql`: CREATE TABLE tenants (id VARCHAR PK, name, industry, theme JSONB, enabled_modules JSONB, settings JSONB, active BOOLEAN, timestamps). NO tenant_id, NO RLS.
- [x] **Tenant entity** — `Tenant.java` (replaced dead `TenantConfig.java`). String PK, @JdbcTypeCode(SqlTypes.JSON) for JSONB fields, @PrePersist/@PreUpdate timestamps.
- [x] **TenantConfigResponse DTO** — record with TenantTheme sub-record. `from(Tenant)` mapper extracts theme keys.
- [x] **CreateTenantRequest + UpdateTenantRequest DTOs** — Jakarta validation on create, nullable fields on update.
- [x] **TenantRepository** — PanacheRepositoryBase<Tenant, String>
- [x] **TenantConfigService** — findConfig, create, update, delete. Uses EntityNotFoundException for 404.
- [x] **TenantConfigController** — GET /{tenantId}/config (@PermitAll) + GET/POST/PUT/DELETE admin (@RolesAllowed ADMIN)
- [x] **Reemplazar TenantConfig.java** — dead record replaced with Tenant entity
- [x] **Integration test** — TenantConfigTest: 7 tests (create, get config shape, 404, update, verify update, list, delete)
- [x] **Seed data** — V5 migration: go-bar (COMERCIANTES) + central-de-bebidas (DISTRIBUIDORES)

#### Decisiones de diseno clave

1. **Tabla `tenants` NO tiene tenant_id** — es el registro maestro, no pertenece a ningun tenant
2. **NO extiende BaseEntity** — PK es String slug (ej: "go-bar"), no UUID
3. **NO tiene RLS** — todos los tenants son visibles para el sistema (el endpoint filtra por ID)
4. **@PermitAll en GET config** — el frontend lo llama antes del login completo
5. **theme y settings son JSONB** — extensibles sin migraciones futuras
6. **enabledModules es JSONB array** — List<String> en Java, array de strings en PostgreSQL

#### Archivos a crear/modificar

| Archivo | Accion |
|---------|--------|
| `demeter-app/src/main/resources/db/migration/V4__create_tenants_table.sql` | CREATED |
| `demeter-common/.../tenant/Tenant.java` | CREATED (replaced TenantConfig.java) |
| `demeter-common/.../tenant/TenantRepository.java` | CREATED |
| `demeter-common/.../tenant/TenantConfigResponse.java` | CREATED (DTO) |
| `demeter-common/.../tenant/TenantTheme.java` | CREATED (DTO) |
| `demeter-common/.../tenant/CreateTenantRequest.java` | CREATED (DTO) |
| `demeter-common/.../tenant/UpdateTenantRequest.java` | CREATED (DTO) |
| `demeter-common/.../tenant/TenantConfigService.java` | CREATED |
| `demeter-common/.../tenant/TenantConfigController.java` | CREATED |
| `demeter-app/src/test/.../integration/TenantConfigTest.java` | CREATED |

---

## Build Status

| Phase | Status | Build |
|-------|--------|-------|
| FASE 1 | DONE | BUILD SUCCESSFUL |
| FASE 2 | DONE | BUILD SUCCESSFUL |
| FASE 3 | DONE | BUILD SUCCESSFUL (2 cross-module fixes applied) |
| FASE 4 | DONE | BUILD SUCCESSFUL |
| FASE 5 | DONE | ALL 42 TESTS PASSING |
| FASE 6 | DONE | ALL 49 TESTS PASSING (42 + 7 TenantConfigTest) |

---

## Module Dependency Graph

```
demeter-app (Quarkus main)
├── demeter-analytics ── productos, inventario, ventas, costos, ubicaciones, empaquetado, precios
├── demeter-ventas ───── productos, inventario
├── demeter-costos ───── productos, inventario
├── demeter-precios ──── productos
├── demeter-inventario ─ productos
├── demeter-fotos ────── productos
├── demeter-chatbot ──── (common only)
├── demeter-productos ── (common only)
├── demeter-ubicaciones ─ (common only)
├── demeter-empaquetado ─ (common only)
├── demeter-usuarios ─── (common only)
└── demeter-common ───── (base module: BaseEntity, tenant, auth, exceptions)
```

---

## API Endpoints Summary

| Module | Base Path | Methods |
|--------|-----------|---------|
| Productos | `/api/v1/products` | CRUD |
| Productos | `/api/v1/categories` | CRUD |
| Productos | `/api/v1/families` | CRUD |
| Ubicaciones | `/api/v1/warehouses` | CRUD + soft delete |
| Ubicaciones | `/api/v1/storage-areas` | CRUD |
| Ubicaciones | `/api/v1/storage-locations` | CRUD |
| Ubicaciones | `/api/v1/storage-bins` | CRUD |
| Ubicaciones | `/api/v1/storage-bin-types` | CRUD |
| Empaquetado | `/api/v1/packaging-types` | CRUD |
| Empaquetado | `/api/v1/packaging-materials` | CRUD |
| Empaquetado | `/api/v1/packaging-colors` | CRUD |
| Empaquetado | `/api/v1/packaging-catalogs` | CRUD |
| Usuarios | `/api/v1/users` | CRUD + roles |
| Inventario | `/api/v1/stock-batches` | CRUD + filters |
| Inventario | `/api/v1/stock-movements` | CRUD + batch associations |
| Ventas | `/api/v1/sales` | CRUD + complete/cancel |
| Costos | `/api/v1/costs` | CRUD + by-product/by-batch |
| Precios | `/api/v1/price-lists` | CRUD + activate/deactivate |
| Precios | `/api/v1/price-lists/{id}/entries` | CRUD + bulk |
| Analytics | `/api/v1/analytics/*` | 7 read-only endpoints |
| Fotos | `/api/v1/photo-sessions` | CRUD + status polling |
| Fotos | `/api/v1/images` | read + detections/classifications |
| Chatbot | `/api/v1/chat/sessions` | CRUD + messages |
| Chatbot | `/api/v1/chat/messages` | read + tool executions |
| Tenants | `/api/v1/tenants/{id}/config` | GET (@PermitAll) + CRUD admin (@RolesAllowed ADMIN) |

---

## Issues Resolved During Build

1. **TenantResolver filename mismatch** — class `DemeterTenantResolver` was in file `TenantResolver.java`
2. **Deprecated config keys** — removed `quarkus.http.cors=true` and `quarkus.health.extensions.enabled`
3. **Java version upgrade** — changed from Java 21 to Java 25 (SDKMAN)
4. **Gradle version upgrade** — changed from 6.8.1 to 9.3.1 (SDKMAN)
5. **SaleCompletionService** — called non-existent `createSaleMovement()`, fixed to use `create(CreateStockMovementRequest)`
6. **AnalyticsService** — 8 type mismatches (String vs enum, missing getters), all fixed
7. **@TestSecurity for integration tests** — controllers use `@RolesAllowed`, added `@TestSecurity(user, roles=ADMIN)` to 4 test classes + `quarkus-test-security` dependency
8. **DemeterTenantResolver JWT guard** — `JsonWebToken` proxy throws `IllegalStateException` when principal is not a real JWT (e.g. `@TestSecurity`), wrapped in try-catch
9. **RlsConnectionCustomizer SET syntax** — PostgreSQL `SET` doesn't support `$1` parameters, changed to `SELECT set_config()` which does
10. **MultiTenantIsolationTest cross-test contamination** — shared tenant IDs caused count mismatches, gave each test method unique tenant IDs
11. **SaleFlowTest quantity assertion** — `BigDecimal("10")` serializes as integer `10`, not `10.0f`, fixed float comparison
