# Demeter AI 2.0 — Deployment Guide

## Prerequisites

- [Google Cloud SDK](https://cloud.google.com/sdk/docs/install) installed and configured
- [Terraform](https://developer.hashicorp.com/terraform/downloads) >= 1.5
- GCP Project with billing enabled
- Auth0 tenant configured (or other OIDC provider)

## GCP APIs Required

Enable these APIs in your GCP project:

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

## Initial Setup

### 1. Create Terraform State Bucket

```bash
PROJECT_ID=$(gcloud config get-value project)
gsutil mb -l us-central1 gs://${PROJECT_ID}-terraform-state
gsutil versioning set on gs://${PROJECT_ID}-terraform-state
```

Update `versions.tf` with your bucket name if different.

### 2. Configure Variables

```bash
cd deploy/terraform
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your values
```

### 3. Initialize Terraform

```bash
terraform init
```

### 4. Review Plan

```bash
terraform plan
```

### 5. Apply Infrastructure

```bash
terraform apply
```

This creates:
- VPC with private subnet and VPC Access Connector
- Cloud SQL PostgreSQL 17 instance with private IP
- Artifact Registry for Docker images
- Secret Manager secrets for DB and OIDC
- Cloud Run service (initially with placeholder image)

## Building and Deploying

### Option A: Manual Deploy

```bash
# Build and push image
docker build -t us-central1-docker.pkg.dev/${PROJECT_ID}/demeter/backend:latest .
docker push us-central1-docker.pkg.dev/${PROJECT_ID}/demeter/backend:latest

# Deploy to Cloud Run
gcloud run deploy demeter-backend-prod \
  --image us-central1-docker.pkg.dev/${PROJECT_ID}/demeter/backend:latest \
  --region us-central1
```

### Option B: Cloud Build (CI/CD)

Set up a trigger in Cloud Build:

```bash
gcloud builds triggers create github \
  --repo-name=StockControl-Backend \
  --repo-owner=YOUR_GITHUB_ORG \
  --branch-pattern=^main$ \
  --build-config=deploy/cloudbuild.yaml \
  --substitutions=_ENVIRONMENT=prod
```

## Database Migrations

Flyway migrations run automatically on application startup. The migrations are located in:
`demeter-app/src/main/resources/db/migration/`

## Environment-Specific Configurations

| Environment | Profile | Min Instances | DB Availability |
|-------------|---------|---------------|-----------------|
| prod        | prod    | 0 (scale-to-zero) | REGIONAL |
| staging     | staging | 0 | ZONAL |

## Monitoring

- **Cloud Run**: Console > Cloud Run > demeter-backend-{env}
- **Cloud SQL**: Console > SQL > demeter-db-{env}
- **Logs**: Console > Logging > demeter-backend

## Troubleshooting

### Connection to Cloud SQL fails

1. Verify VPC Connector is running:
   ```bash
   gcloud compute networks vpc-access connectors describe demeter-vpc-connector --region=us-central1
   ```

2. Check Cloud Run has Cloud SQL client role:
   ```bash
   gcloud projects get-iam-policy ${PROJECT_ID} \
     --flatten="bindings[].members" \
     --filter="bindings.role:roles/cloudsql.client"
   ```

### Secrets not accessible

Verify IAM bindings:
```bash
gcloud secrets get-iam-policy demeter-db-url-prod
```

### Startup probe failing

Check application logs:
```bash
gcloud run services logs read demeter-backend-prod --region=us-central1
```

## Costs Estimate (Monthly)

| Resource | Configuration | Est. Cost |
|----------|---------------|-----------|
| Cloud Run | 0-10 instances, 2 vCPU, 1GB | $0-50 |
| Cloud SQL | db-custom-2-4096, 20GB SSD | ~$70 |
| VPC Connector | 2 instances | ~$15 |
| Artifact Registry | <10GB | ~$1 |
| **Total** | | **~$90-140** |

*Costs vary by usage. Scale-to-zero Cloud Run minimizes costs during low traffic.*

## Custom Domain Setup

### 1. Verify Domain Ownership

```bash
# Option A: Via Google Search Console
# Go to https://search.google.com/search-console and add your domain

# Option B: Via gcloud (if using Cloud Identity)
gcloud domains verify api.demeter.app
```

### 2. Configure DNS

Add a CNAME record in your DNS provider:

| Type | Name | Value |
|------|------|-------|
| CNAME | api | ghs.googlehosted.com. |

### 3. Enable Domain Mapping in Terraform

```hcl
# In terraform.tfvars
custom_domain = "api.demeter.app"
```

### 4. Apply and Wait for SSL

```bash
terraform apply

# SSL certificate provisioning takes 15-30 minutes
# Check status:
gcloud run domain-mappings describe --domain=api.demeter.app --region=us-central1
```

## Security Checklist

- [ ] DB password is strong (32+ chars)
- [ ] OIDC provider configured correctly
- [ ] CORS origins restricted to frontend domain
- [ ] Cloud SQL has deletion protection enabled
- [ ] Secrets rotated periodically
- [ ] VPC has no public IP on Cloud SQL
- [ ] Custom domain has valid SSL certificate
