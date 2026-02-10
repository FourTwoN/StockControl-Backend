# =============================================================================
# Custom Domain Mapping for Cloud Run
# =============================================================================
# Prerequisites:
#   1. Domain verified in Google Search Console or Cloud Identity
#   2. DNS CNAME record: api.demeter.app -> ghs.googlehosted.com
# =============================================================================

resource "google_cloud_run_domain_mapping" "api" {
  count    = var.custom_domain != "" ? 1 : 0
  location = var.region
  name     = var.custom_domain

  metadata {
    namespace = var.project_id
  }

  spec {
    route_name = google_cloud_run_v2_service.backend.name
  }

  depends_on = [google_cloud_run_v2_service.backend]
}

# =============================================================================
# Output DNS instructions
# =============================================================================

output "domain_mapping_status" {
  description = "Domain mapping resource status"
  value       = var.custom_domain != "" ? google_cloud_run_domain_mapping.api[0].status : null
}

output "dns_records_required" {
  description = "DNS records to configure for custom domain"
  value = var.custom_domain != "" ? {
    type  = "CNAME"
    name  = var.custom_domain
    value = "ghs.googlehosted.com."
    note  = "Add this CNAME record in your DNS provider"
  } : null
}
