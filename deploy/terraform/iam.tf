# =============================================================================
# Service Account for Cloud Run
# =============================================================================

resource "google_service_account" "cloudrun" {
  account_id   = "demeter-cloudrun-${var.environment}"
  display_name = "Demeter Cloud Run Service Account (${var.environment})"
}

# =============================================================================
# IAM — Secret Manager Access
# =============================================================================

resource "google_secret_manager_secret_iam_member" "db_url_access" {
  secret_id = google_secret_manager_secret.db_url.secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.cloudrun.email}"
}

resource "google_secret_manager_secret_iam_member" "db_user_access" {
  secret_id = google_secret_manager_secret.db_user.secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.cloudrun.email}"
}

resource "google_secret_manager_secret_iam_member" "db_password_access" {
  secret_id = google_secret_manager_secret.db_password.secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.cloudrun.email}"
}

resource "google_secret_manager_secret_iam_member" "oidc_auth_server_url_access" {
  secret_id = google_secret_manager_secret.oidc_auth_server_url.secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.cloudrun.email}"
}

resource "google_secret_manager_secret_iam_member" "oidc_client_id_access" {
  secret_id = google_secret_manager_secret.oidc_client_id.secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.cloudrun.email}"
}

resource "google_secret_manager_secret_iam_member" "oidc_issuer_access" {
  secret_id = google_secret_manager_secret.oidc_issuer.secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.cloudrun.email}"
}

# =============================================================================
# IAM — Cloud SQL Client Access
# =============================================================================

resource "google_project_iam_member" "cloudrun_cloudsql" {
  project = var.project_id
  role    = "roles/cloudsql.client"
  member  = "serviceAccount:${google_service_account.cloudrun.email}"
}
