# =============================================================================
# Cloud Run Outputs
# =============================================================================

output "cloud_run_url" {
  description = "URL of the deployed Cloud Run service"
  value       = google_cloud_run_v2_service.backend.uri
}

output "cloud_run_service_name" {
  description = "Name of the Cloud Run service"
  value       = google_cloud_run_v2_service.backend.name
}

# =============================================================================
# Cloud SQL Outputs
# =============================================================================

output "db_connection_name" {
  description = "Cloud SQL connection name for Cloud Run"
  value       = google_sql_database_instance.demeter.connection_name
}

output "db_private_ip" {
  description = "Private IP address of Cloud SQL instance"
  value       = google_sql_database_instance.demeter.private_ip_address
}

# =============================================================================
# Artifact Registry Outputs
# =============================================================================

output "artifact_registry_url" {
  description = "Artifact Registry repository URL"
  value       = "${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.demeter.repository_id}"
}

# =============================================================================
# VPC Outputs
# =============================================================================

output "vpc_id" {
  description = "VPC network ID"
  value       = google_compute_network.vpc.id
}

output "vpc_connector_id" {
  description = "VPC Access Connector ID for Cloud Run"
  value       = google_vpc_access_connector.connector.id
}
