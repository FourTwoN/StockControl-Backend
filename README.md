# Demeter AI 2.0 — Backend

**Multi-tenant stock control and analytics platform for same-industry SaaS.**

Java 25 &middot; Quarkus 3.31.2 &middot; PostgreSQL 17 &middot; Gradle 9.3.1

---

## What is Demeter?

Demeter is a backend platform designed so that **multiple businesses within the same industry** share a single codebase and database while keeping their data, catalogs, workflows, and configurations completely isolated. Each tenant (business) gets its own universe of products, warehouses, pricing, and analytics — without needing a separate deployment.

### Who is it for?

Any industry where multiple companies manage physical stock: agriculture, food production, chemicals, retail, manufacturing, logistics, pharmaceuticals. The platform adapts to each tenant's specific vocabulary, processes, and data requirements.

---

## Architecture

### 13 Modules

```
demeter-app (Quarkus main)
├── demeter-productos      Products, categories, families, density parameters
├── demeter-inventario     Stock batches, movements, custom attributes
├── demeter-ventas         Sales lifecycle (create → complete/cancel)
├── demeter-costos         Cost tracking (product/batch level)
├── demeter-precios        Price lists with versioning and tiered pricing
├── demeter-ubicaciones    Warehouses, areas, locations, bins (custom hierarchy)
├── demeter-empaquetado    Packaging types, materials, colors, catalogs
├── demeter-usuarios       Users, roles, OIDC mapping
├── demeter-analytics      7 read-only dashboard endpoints
├── demeter-fotos          Photo processing sessions, AI detections (DLC)
├── demeter-chatbot        Conversational AI with tool execution (DLC)
└── demeter-common         BaseEntity, tenant resolver, RLS, auth, exceptions
```

### 3-Layer Tenant Isolation

Every row in the database belongs to exactly one tenant. Isolation is enforced at three independent levels — all three must fail simultaneously for data to leak:

| Layer | Mechanism | What it does |
|-------|-----------|-------------|
| **Application** | `DemeterTenantResolver` | Extracts `tenant_id` from JWT claim or `X-Tenant-ID` header |
| **ORM** | Hibernate `@TenantId` discriminator | Appends `WHERE tenant_id = ?` to every query automatically |
| **Database** | PostgreSQL Row-Level Security | RLS policies on all 40+ tables enforce `tenant_id = current_setting('app.current_tenant')` |

```
Request → JWT/Header → TenantResolver → TenantContext (request-scoped)
                                              │
                        ┌─────────────────────┼─────────────────────┐
                        ▼                     ▼                     ▼
                   Hibernate              set_config()          RLS Policy
                   @TenantId           app.current_tenant     USING(tenant_id)
                   (ORM filter)        (connection var)       (DB enforcement)
```

---

## How Customizable Is It Per Tenant?

This is the core design question. Demeter is built so that **two competing businesses in the same industry** can use the same deployment with completely different configurations. Here's exactly what each tenant controls independently:

### 1. Catalogs and Master Data

Every catalog entity is tenant-scoped. Tenant A's product categories are invisible to Tenant B.

| Catalog | What it means |
|---------|--------------|
| **Product categories & families** | Tenant A: "Solvents", "Acids", "Bases". Tenant B: "Dairy", "Grains", "Produce" |
| **Packaging types, materials, colors** | Tenant A: "Drum 200L", "Steel", "Blue". Tenant B: "Crate 10kg", "Wood", "Natural" |
| **Warehouse bin types** | Tenant A: "Cold room rack", "Hazmat shelf". Tenant B: "Pallet bay", "Picking bin" |
| **Density parameters** | Industry-specific conversion factors per product |

Each tenant builds their own taxonomy from scratch. The system imposes no predefined categories.

### 2. JSONB Custom Attributes (schema-free extension)

`StockBatch` entities carry a `custom_attributes` JSONB column that accepts **arbitrary structured data per tenant** without schema migrations:

**Tenant A (Chemical distributor):**
```json
{
  "supplier": "Chem Corp Ltd",
  "hazmatCode": "UN1234",
  "certificationType": "ISO-14001",
  "temperatureControlled": true,
  "lotNumber": "LOT-2024-003456"
}
```

**Tenant B (Agricultural cooperative):**
```json
{
  "origin": "Mendoza, Argentina",
  "organic": true,
  "harvestDate": "2024-03-15",
  "moisture": 12.5,
  "grainType": "Triticum aestivum"
}
```

This is the most powerful customization mechanism: **unlimited tenant-specific fields with zero ALTER TABLE and zero code changes**. PostgreSQL JSONB supports indexing and querying these fields.

### 3. Warehouse Layout (custom depth)

Each tenant defines their own physical layout hierarchy:

```
Tenant A (simple):
  Warehouse "Main" → 3 bins

Tenant B (complex):
  Warehouse "Plant Norte" → Area "Cold Storage" → Location "Rack A" → Bin "A-01-03"
  Warehouse "Plant Norte" → Area "Dry Storage"  → Location "Floor 2"  → Bin "F2-15"
  Warehouse "Depot Sur"   → Area "Loading Dock" → ...
```

Warehouses, areas, locations, bins, and bin types are all tenant-scoped. Bin types (dimensions, capacity, constraints) are defined per tenant.

### 4. Pricing Models

Each tenant manages independent price lists with versioning:

- Multiple concurrent price lists (wholesale, retail, VIP, regional)
- Effective dates for version control (price changes without deleting history)
- Per-product price entries with quantity tiers
- Activate/deactivate lists without deletion

Tenant A may have 1 simple price list. Tenant B may have 15 lists covering different regions and customer tiers.

### 5. Cost Tracking

Costs are tracked per product or per batch, with tenant-defined cost types:

- Tenant A: "raw material", "transport", "storage"
- Tenant B: "seeds", "fertilizer", "labor", "packaging", "freight", "cold chain"

Currency is a string field — multi-currency is supported per transaction.

### 6. Workflow and Status Rules

Sale lifecycle follows a state machine (PENDING → COMPLETED / CANCELLED), but each transition is validated at the service layer. Stock batch statuses (ACTIVE, DEPLETED, EXPIRED, QUARANTINED) are enum-based but extensible.

### 7. Module Selection

Not every tenant needs every feature. Modules can be enabled/disabled:

```bash
# A small retailer needs only products, inventory, and users
./gradlew :demeter-app:quarkusDev -Pdemeter.modules=productos,inventario,usuarios

# A large manufacturer needs everything including AI
./gradlew :demeter-app:quarkusDev  # all modules by default
```

DLC modules (`fotos`, `chatbot`) are optional add-ons for tenants that need AI image processing or conversational interfaces.

### 8. Per-Tenant Unique Constraints

SKUs, sale numbers, batch codes, and user emails are unique **within a tenant**, not globally:

```sql
UNIQUE(tenant_id, sku)          -- Tenant A and B can both have SKU "PROD-001"
UNIQUE(tenant_id, sale_number)  -- Independent sale number sequences
UNIQUE(tenant_id, batch_code)   -- Independent batch codes
UNIQUE(tenant_id, email)        -- Same person can exist in multiple tenants
```

### Customization Summary

| Dimension | Freedom | Mechanism |
|-----------|---------|-----------|
| Product taxonomy | Unlimited | Tenant-scoped categories, families |
| Batch metadata | Unlimited | JSONB `custom_attributes` |
| Warehouse layout | Unlimited | Hierarchical entities, custom bin types |
| Pricing | Unlimited | Multiple versioned price lists |
| Cost types | Unlimited | Tenant-defined string types |
| Packaging catalogs | Unlimited | Tenant-scoped types, materials, colors |
| Units of measure | Unlimited | String field per record (kg, liters, boxes, pallets) |
| Currency | Unlimited | String field per transaction |
| Feature set | Configurable | Module selection at build/runtime |
| User roles | 3 levels | VIEWER, EDITOR, ADMIN (RBAC) |

---

## Quick Start

### Prerequisites

- Java 25 (SDKMAN: `sdk install java 25-open`)
- Docker (for PostgreSQL via Testcontainers/DevServices)

### Run in development

```bash
# All modules
./gradlew :demeter-app:quarkusDev

# Specific modules
./gradlew :demeter-app:quarkusDev -Pdemeter.modules=productos,inventario,ventas

# Check active modules
./gradlew :demeter-app:printDemeterModules
```

Quarkus DevServices automatically starts a PostgreSQL 17 container. No manual database setup needed.

### Run tests

```bash
./gradlew :demeter-app:test
```

42 integration tests covering:

| Suite | Tests | Coverage |
|-------|-------|---------|
| HealthCheckTest | 3 | Liveness, readiness, overall health |
| ProductControllerTest | 7 | CRUD + validation + 404 handling |
| StockBatchControllerTest | 10 | CRUD + filters by product/status + transitions |
| SaleFlowTest | 14 | Full lifecycle: product → batch → sale → complete/cancel |
| MultiTenantIsolationTest | 3 | Tenant A/B data isolation (products, batches, sales) |
| OpenApiTest | 5 | OpenAPI spec generation + Swagger UI |

### API documentation

With the app running:
- Swagger UI: `http://localhost:8080/q/swagger-ui/`
- OpenAPI spec: `http://localhost:8080/q/openapi`

### Example request

```bash
curl -X POST http://localhost:8080/api/v1/products \
  -H "X-Tenant-ID: my-tenant" \
  -H "Content-Type: application/json" \
  -d '{"sku": "PROD-001", "name": "Widget Alpha", "description": "A test product"}'
```

---

## API Endpoints

| Module | Path | Methods |
|--------|------|---------|
| Productos | `/api/v1/products` | CRUD |
| Productos | `/api/v1/categories` | CRUD |
| Productos | `/api/v1/families` | CRUD |
| Inventario | `/api/v1/stock-batches` | CRUD + by-product, by-status |
| Inventario | `/api/v1/stock-movements` | CRUD + batch associations |
| Ventas | `/api/v1/sales` | CRUD + complete/cancel |
| Costos | `/api/v1/costs` | CRUD + by-product, by-batch |
| Precios | `/api/v1/price-lists` | CRUD + activate/deactivate |
| Precios | `/api/v1/price-lists/{id}/entries` | CRUD + bulk |
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
| Analytics | `/api/v1/analytics/*` | 7 read-only endpoints |
| Fotos | `/api/v1/photo-sessions` | CRUD + status polling |
| Fotos | `/api/v1/images` | Read + detections/classifications |
| Chatbot | `/api/v1/chat/sessions` | CRUD + messages |
| Chatbot | `/api/v1/chat/messages` | Read + tool executions |

---

## Production Deployment (Google Cloud Platform)

This project is designed for deployment on **Google Cloud Platform** using Cloud Run (serverless containers) and Cloud SQL (managed PostgreSQL). The infrastructure is fully defined as code using Terraform.

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              GOOGLE CLOUD PLATFORM                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────┐      ┌─────────────────────────────────────────────────┐  │
│  │   Client    │      │                  VPC Network                    │  │
│  │  (Browser)  │      │                 demeter-vpc                     │  │
│  └──────┬──────┘      │  ┌───────────────────────────────────────────┐  │  │
│         │             │  │           Subnet 10.0.0.0/24              │  │  │
│         ▼             │  │                                           │  │  │
│  ┌──────────────┐     │  │  ┌─────────────┐    ┌─────────────────┐  │  │  │
│  │ Cloud Run    │     │  │  │ VPC Access  │    │   Cloud SQL     │  │  │  │
│  │ (Public URL) │─────┼──┼─▶│ Connector   │───▶│  PostgreSQL 17  │  │  │  │
│  │              │     │  │  │ 10.8.0.0/28 │    │  (Private IP)   │  │  │  │
│  │ demeter-     │     │  │  └─────────────┘    └─────────────────┘  │  │  │
│  │ backend-prod │     │  │                                           │  │  │
│  └──────┬───────┘     │  └───────────────────────────────────────────┘  │  │
│         │             │                                                  │  │
│         │             └──────────────────────────────────────────────────┘  │
│         │                                                                    │
│         ▼                                                                    │
│  ┌──────────────┐     ┌──────────────┐     ┌──────────────────────────┐    │
│  │ Domain       │     │ Artifact     │     │     Secret Manager       │    │
│  │ Mapping      │     │ Registry     │     │                          │    │
│  │              │     │              │     │ • demeter-db-url         │    │
│  │ api.demeter  │     │ demeter/     │     │ • demeter-db-user        │    │
│  │ .app         │     │ backend:tag  │     │ • demeter-db-password    │    │
│  └──────────────┘     └──────────────┘     │ • demeter-oidc-*         │    │
│                                             └──────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Infrastructure Components

| Component | Resource | Configuration |
|-----------|----------|---------------|
| **Compute** | Cloud Run v2 | 0-10 instances, 2 vCPU, 1GB RAM, scale-to-zero |
| **Database** | Cloud SQL PostgreSQL 17 | db-custom-2-4096, 20GB SSD, private IP only |
| **Networking** | VPC + Subnet | 10.0.0.0/24, private service connection |
| **Connectivity** | VPC Access Connector | 10.8.0.0/28, 2-3 instances |
| **Secrets** | Secret Manager | 6 secrets (DB + OIDC credentials) |
| **Registry** | Artifact Registry | Docker format, 30-day cleanup policy |
| **Domain** | Cloud Run Domain Mapping | Custom domain with managed SSL |
| **CI/CD** | Cloud Build | Triggered on push to main |

### Terraform File Structure

All infrastructure code lives in `deploy/terraform/`:

```
deploy/terraform/
├── versions.tf              # Terraform 1.5+, Google provider ~5.0
├── variables.tf             # All input variables with validation
├── outputs.tf               # URLs, IPs, connection strings
├── vpc.tf                   # VPC, subnet, private service connection
├── cloudsql.tf              # PostgreSQL instance, database, user
├── secrets.tf               # Secret Manager resources
├── iam.tf                   # Service account + IAM bindings
├── artifact-registry.tf     # Docker image repository
├── cloudrun.tf              # Cloud Run service configuration
├── domain.tf                # Custom domain mapping
├── terraform.tfvars.example # Template for your values
└── .gitignore               # Protects sensitive files
```

### Key Design Decisions

#### 1. Private Database Connectivity

Cloud SQL has **no public IP**. All connections go through the VPC:

```hcl
# cloudsql.tf
ip_configuration {
  ipv4_enabled    = false           # No public IP
  private_network = google_compute_network.vpc.id
}
```

Cloud Run connects via **VPC Access Connector**:

```hcl
# cloudrun.tf
vpc_access {
  connector = google_vpc_access_connector.connector.id
  egress    = "PRIVATE_RANGES_ONLY"  # Only private traffic through VPC
}
```

#### 2. Cloud SQL Socket Factory

The application uses Google's Cloud SQL Socket Factory for secure connections without managing SSL certificates:

```kotlin
// demeter-common/build.gradle.kts
api("com.google.cloud.sql:postgres-socket-factory:1.21.0")
```

JDBC URL format for Cloud Run:
```
jdbc:postgresql:///demeter?cloudSqlInstance=PROJECT:REGION:INSTANCE&socketFactory=com.google.cloud.sql.postgres.SocketFactory
```

#### 3. Secrets Management

All sensitive values are stored in **Secret Manager** and injected as environment variables:

| Secret | Purpose | Used By |
|--------|---------|---------|
| `demeter-db-url-{env}` | JDBC connection string with socket factory | Cloud Run |
| `demeter-db-user-{env}` | Database username | Cloud Run |
| `demeter-db-password-{env}` | Database password | Cloud Run |
| `demeter-oidc-auth-server-url-{env}` | Auth0 tenant URL | Cloud Run |
| `demeter-oidc-client-id-{env}` | Auth0 application ID | Cloud Run |
| `demeter-oidc-issuer-{env}` | JWT issuer URL | Cloud Run |

Secrets are accessed via IAM bindings, not embedded in config:

```hcl
# iam.tf
resource "google_secret_manager_secret_iam_member" "db_password_access" {
  secret_id = google_secret_manager_secret.db_password.secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.cloudrun.email}"
}
```

#### 4. Scale-to-Zero with Fast Startup

Cloud Run is configured for cost optimization:

```hcl
# cloudrun.tf
scaling {
  min_instance_count = 0   # Scale to zero when idle
  max_instance_count = 10  # Handle traffic spikes
}

resources {
  cpu_idle          = true   # Release CPU when idle
  startup_cpu_boost = true   # Extra CPU during cold start
}
```

Quarkus **fast-jar** packaging (not uber-jar) is used for faster cold starts:

```dockerfile
# Dockerfile
RUN gradle :demeter-app:quarkusBuild --no-daemon  # Produces fast-jar by default
```

#### 5. Health Probes

Cloud Run uses Quarkus health endpoints for container lifecycle:

```hcl
# cloudrun.tf
startup_probe {
  http_get {
    path = "/q/health/started"
    port = 8080
  }
  initial_delay_seconds = 5
  period_seconds        = 3
  failure_threshold     = 30  # 90 seconds max startup time
}

liveness_probe {
  http_get {
    path = "/q/health/live"
    port = 8080
  }
  period_seconds = 30
}
```

#### 6. Database Configuration

PostgreSQL 17 with production-ready settings:

```hcl
# cloudsql.tf
settings {
  tier              = var.db_tier           # db-custom-2-4096 (2 vCPU, 4GB)
  availability_type = "REGIONAL"            # High availability (prod)
  disk_type         = "PD_SSD"              # SSD for performance
  disk_autoresize   = true                  # Auto-grow storage

  database_flags {
    name  = "max_connections"
    value = "200"
  }

  database_flags {
    name  = "cloudsql.enable_pg_vector"     # For future AI features
    value = "on"
  }

  backup_configuration {
    enabled                        = true
    point_in_time_recovery_enabled = true   # PITR for prod
    start_time                     = "03:00"
    transaction_log_retention_days = 7

    backup_retention_settings {
      retained_backups = 30                 # 30 days of backups
    }
  }

  insights_config {
    query_insights_enabled = true           # Query performance monitoring
  }
}

deletion_protection = true                  # Prevent accidental deletion
```

### Deployment Steps

#### Prerequisites

1. **GCP Project** with billing enabled
2. **gcloud CLI** installed and authenticated
3. **Terraform** >= 1.5
4. **Auth0 tenant** with API and Application configured
5. **Domain** verified in Google Search Console (for custom domain)

#### 1. Enable Required APIs

```bash
gcloud services enable \
  run.googleapis.com \
  sqladmin.googleapis.com \
  secretmanager.googleapis.com \
  artifactregistry.googleapis.com \
  vpcaccess.googleapis.com \
  servicenetworking.googleapis.com \
  cloudbuild.googleapis.com \
  compute.googleapis.com
```

#### 2. Create Terraform State Bucket

```bash
PROJECT_ID=$(gcloud config get-value project)
gsutil mb -l us-central1 gs://${PROJECT_ID}-terraform-state
gsutil versioning set on gs://${PROJECT_ID}-terraform-state
```

#### 3. Configure Variables

```bash
cd deploy/terraform
cp terraform.tfvars.example terraform.tfvars
```

Edit `terraform.tfvars`:

```hcl
# GCP Configuration
project_id  = "my-gcp-project"
region      = "us-central1"
environment = "prod"

# Database (generate: openssl rand -base64 32)
db_password = "super-secure-password-here"

# Auth0 Configuration
oidc_auth_server_url = "https://my-tenant.auth0.com/"
oidc_client_id       = "abc123def456"
oidc_issuer          = "https://my-tenant.auth0.com/"

# Custom Domain (optional)
custom_domain = "api.demeter.app"

# CORS (frontend domains)
cors_origins = "https://cultivos.demeter.app,https://vending.demeter.app"
```

#### 4. Deploy Infrastructure

```bash
terraform init
terraform plan    # Review changes
terraform apply   # Create resources (takes ~10 minutes)
```

#### 5. Configure DNS (for custom domain)

Add CNAME record in your DNS provider:

| Type | Name | Value | TTL |
|------|------|-------|-----|
| CNAME | api | ghs.googlehosted.com. | 300 |

SSL certificate is automatically provisioned (15-30 minutes).

#### 6. Build and Deploy Application

**Option A: Manual**

```bash
# Build and push image
docker build -t us-central1-docker.pkg.dev/${PROJECT_ID}/demeter/backend:latest .
docker push us-central1-docker.pkg.dev/${PROJECT_ID}/demeter/backend:latest

# Deploy
gcloud run deploy demeter-backend-prod \
  --image us-central1-docker.pkg.dev/${PROJECT_ID}/demeter/backend:latest \
  --region us-central1
```

**Option B: Cloud Build (CI/CD)**

Push to main branch triggers automatic build and deploy via `deploy/cloudbuild.yaml`.

### CI/CD Pipeline (Cloud Build)

The pipeline (`deploy/cloudbuild.yaml`) runs on every push to main:

```yaml
steps:
  # 1. Run tests
  - name: "gradle:9.3-jdk25"
    args: ["test", "--no-daemon", "--parallel"]

  # 2. Build Docker image
  - name: "gcr.io/cloud-builders/docker"
    args: ["build", "-t", "...", "."]

  # 3. Push to Artifact Registry
  - name: "gcr.io/cloud-builders/docker"
    args: ["push", "--all-tags", "..."]

  # 4. Deploy to Cloud Run
  - name: "gcr.io/google.com/cloudsdktool/cloud-sdk"
    args: ["run", "deploy", "demeter-backend-prod", ...]
```

### Environment Variables

The application reads these environment variables in production:

| Variable | Source | Purpose |
|----------|--------|---------|
| `QUARKUS_PROFILE` | Terraform | Activates `%prod` config prefix |
| `DB_URL` | Secret Manager | JDBC connection string |
| `DB_USER` | Secret Manager | Database username |
| `DB_PASSWORD` | Secret Manager | Database password |
| `OIDC_AUTH_SERVER_URL` | Secret Manager | Auth0 server URL |
| `OIDC_CLIENT_ID` | Secret Manager | Auth0 client ID |
| `OIDC_ISSUER` | Secret Manager | JWT issuer for validation |
| `CORS_ORIGINS` | Terraform | Allowed frontend origins |

### Authentication (Auth0)

Production uses **Auth0** for OIDC authentication:

```properties
# application.properties
%prod.quarkus.oidc.auth-server-url=${OIDC_AUTH_SERVER_URL}
%prod.quarkus.oidc.client-id=${OIDC_CLIENT_ID}
%prod.quarkus.oidc.token.issuer=${OIDC_ISSUER}
quarkus.oidc.application-type=service
quarkus.oidc.roles.role-claim-path=permissions
```

#### Auth0 API Configuration

Create an Auth0 API with these settings:

| Setting | Value |
|---------|-------|
| Identifier (Audience) | `https://api.demeter.app` |
| Token Expiration | 86400 (24 hours) |
| Allow Offline Access | Enabled (for refresh tokens) |

#### Custom Claims

Configure an Auth0 Action to add tenant_id to tokens:

```javascript
exports.onExecutePostLogin = async (event, api) => {
  const namespace = 'https://demeter.app';
  api.accessToken.setCustomClaim(`${namespace}/tenant_id`, event.user.app_metadata.tenant_id);
  api.accessToken.setCustomClaim(`${namespace}/roles`, event.authorization?.roles || []);
};
```

#### Roles Configuration

Map Auth0 permissions to application roles:

| Auth0 Permission | Application Role | Access Level |
|------------------|------------------|--------------|
| `read:all` | VIEWER | Read-only access |
| `write:all` | WORKER | Read + limited write |
| `manage:all` | SUPERVISOR | Full write access |
| `admin:all` | ADMIN | Full access + delete |

### Cost Estimation

| Resource | Configuration | Monthly Cost (USD) |
|----------|---------------|--------------------|
| Cloud Run | 0-10 instances, 2 vCPU, 1GB | $0-50 (usage-based) |
| Cloud SQL | db-custom-2-4096, 20GB SSD | ~$70 |
| VPC Connector | 2 e2-micro instances | ~$15 |
| Secret Manager | 6 secrets, 1000 accesses | ~$0.50 |
| Artifact Registry | <10GB storage | ~$1 |
| Cloud Build | 120 min/month free tier | $0 |
| **Total** | | **~$90-140/month** |

*Scale-to-zero Cloud Run significantly reduces costs during low traffic periods.*

### Monitoring and Logging

#### Cloud Run Metrics
- Request count, latency (p50, p95, p99)
- Instance count, CPU/memory utilization
- Container startup latency

#### Cloud SQL Metrics
- CPU utilization, memory usage
- Disk utilization, I/O operations
- Connection count, query insights

#### Logs
```bash
# Cloud Run logs
gcloud run services logs read demeter-backend-prod --region=us-central1

# Cloud SQL logs (via Cloud Logging)
gcloud logging read "resource.type=cloudsql_database"
```

### Troubleshooting

#### Connection to Cloud SQL fails

1. Verify VPC Connector is running:
   ```bash
   gcloud compute networks vpc-access connectors describe demeter-vpc-connector \
     --region=us-central1
   ```

2. Check service account has Cloud SQL Client role:
   ```bash
   gcloud projects get-iam-policy ${PROJECT_ID} \
     --flatten="bindings[].members" \
     --filter="bindings.role:roles/cloudsql.client"
   ```

3. Verify private service connection:
   ```bash
   gcloud services vpc-peerings list --network=demeter-vpc-prod
   ```

#### Secrets not accessible

```bash
# Check IAM bindings
gcloud secrets get-iam-policy demeter-db-url-prod

# Test secret access
gcloud secrets versions access latest --secret=demeter-db-url-prod
```

#### Startup probe failing

```bash
# Check container logs
gcloud run services logs read demeter-backend-prod --region=us-central1 --limit=50

# Describe service for events
gcloud run services describe demeter-backend-prod --region=us-central1
```

#### Custom domain not working

```bash
# Check domain mapping status
gcloud run domain-mappings describe --domain=api.demeter.app --region=us-central1

# Verify DNS
dig api.demeter.app CNAME
```

### Security Checklist

- [ ] Database password is strong (32+ characters, generated randomly)
- [ ] Cloud SQL has `deletion_protection = true`
- [ ] Cloud SQL has no public IP (`ipv4_enabled = false`)
- [ ] All secrets stored in Secret Manager (not in code or environment)
- [ ] CORS origins restricted to specific frontend domains
- [ ] Auth0 configured with appropriate token expiration
- [ ] VPC firewall rules reviewed
- [ ] Cloud Run service account has minimal permissions
- [ ] Terraform state bucket has versioning enabled
- [ ] `terraform.tfvars` is in `.gitignore`

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 25 |
| Framework | Quarkus 3.31.2 |
| ORM | Hibernate with Panache |
| Database | PostgreSQL 17 |
| Multi-Tenancy | Hibernate Discriminator + PostgreSQL RLS |
| Authentication | OIDC (Auth0) + JWT |
| Authorization | Jakarta `@RolesAllowed` (RBAC) |
| Migrations | Flyway (V1 baseline, V2 indexes, V3 RLS) |
| Build | Gradle 9.3.1 (multi-project) |
| Testing | JUnit 5 + REST Assured + Testcontainers |
| API Docs | SmallRye OpenAPI + Swagger UI |
