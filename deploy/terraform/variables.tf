# =============================================================================
# Project Configuration
# =============================================================================

variable "project_id" {
  type        = string
  description = "GCP Project ID"
}

variable "region" {
  type        = string
  default     = "us-central1"
  description = "GCP Region for all resources"
}

variable "environment" {
  type        = string
  default     = "prod"
  description = "Environment name (prod, staging)"
  validation {
    condition     = contains(["prod", "staging"], var.environment)
    error_message = "Environment must be 'prod' or 'staging'."
  }
}

# =============================================================================
# Database Configuration
# =============================================================================

variable "db_password" {
  type        = string
  sensitive   = true
  description = "PostgreSQL database password for demeter_app user"
}

variable "db_tier" {
  type        = string
  default     = "db-custom-2-4096"
  description = "Cloud SQL machine tier (db-custom-{vCPU}-{RAM_MB})"
}

variable "db_disk_size" {
  type        = number
  default     = 20
  description = "Cloud SQL disk size in GB"
}

variable "db_max_connections" {
  type        = number
  default     = 200
  description = "PostgreSQL max_connections setting"
}

# =============================================================================
# Cloud Run Configuration
# =============================================================================

variable "cloudrun_min_instances" {
  type        = number
  default     = 0
  description = "Minimum Cloud Run instances (0 for scale-to-zero)"
}

variable "cloudrun_max_instances" {
  type        = number
  default     = 10
  description = "Maximum Cloud Run instances"
}

variable "cloudrun_cpu" {
  type        = string
  default     = "2"
  description = "CPU allocation for Cloud Run containers"
}

variable "cloudrun_memory" {
  type        = string
  default     = "1Gi"
  description = "Memory allocation for Cloud Run containers"
}

# =============================================================================
# OIDC / Auth0 Configuration
# =============================================================================

variable "oidc_auth_server_url" {
  type        = string
  description = "OIDC Auth Server URL (e.g., https://your-tenant.auth0.com/)"
}

variable "oidc_client_id" {
  type        = string
  description = "OIDC Client ID"
}

variable "oidc_issuer" {
  type        = string
  description = "OIDC Token Issuer URL"
}

# =============================================================================
# CORS Configuration
# =============================================================================

variable "cors_origins" {
  type        = string
  default     = "https://demeter.42n.io"
  description = "Allowed CORS origins (comma-separated for multiple)"
}

# =============================================================================
# Custom Domain Configuration
# =============================================================================

variable "custom_domain" {
  type        = string
  default     = ""
  description = "Custom domain for API (e.g., api.demeter.app). Leave empty to skip domain mapping."
}

variable "frontend_domains" {
  type        = list(string)
  default     = []
  description = "List of frontend domains for CORS (e.g., [\"cultivos.demeter.app\", \"vending.demeter.app\"])"
}
