# =============================================================================
# Secret Manager — Database Secrets
# =============================================================================

resource "google_secret_manager_secret" "db_url" {
  secret_id = "demeter-db-url-${var.environment}"

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "db_url" {
  secret      = google_secret_manager_secret.db_url.id
  secret_data = "jdbc:postgresql:///${google_sql_database.demeter.name}?cloudSqlInstance=${google_sql_database_instance.demeter.connection_name}&socketFactory=com.google.cloud.sql.postgres.SocketFactory"
}

resource "google_secret_manager_secret" "db_user" {
  secret_id = "demeter-db-user-${var.environment}"

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "db_user" {
  secret      = google_secret_manager_secret.db_user.id
  secret_data = google_sql_user.demeter_app.name
}

resource "google_secret_manager_secret" "db_password" {
  secret_id = "demeter-db-password-${var.environment}"

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "db_password" {
  secret      = google_secret_manager_secret.db_password.id
  secret_data = var.db_password
}

# =============================================================================
# Secret Manager — OIDC Secrets
# =============================================================================

resource "google_secret_manager_secret" "oidc_auth_server_url" {
  secret_id = "demeter-oidc-auth-server-url-${var.environment}"

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "oidc_auth_server_url" {
  secret      = google_secret_manager_secret.oidc_auth_server_url.id
  secret_data = var.oidc_auth_server_url
}

resource "google_secret_manager_secret" "oidc_client_id" {
  secret_id = "demeter-oidc-client-id-${var.environment}"

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "oidc_client_id" {
  secret      = google_secret_manager_secret.oidc_client_id.id
  secret_data = var.oidc_client_id
}

resource "google_secret_manager_secret" "oidc_issuer" {
  secret_id = "demeter-oidc-issuer-${var.environment}"

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "oidc_issuer" {
  secret      = google_secret_manager_secret.oidc_issuer.id
  secret_data = var.oidc_issuer
}
