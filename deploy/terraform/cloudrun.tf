# =============================================================================
# Cloud Run — Backend Service
# =============================================================================

resource "google_cloud_run_v2_service" "backend" {
  name     = "demeter-backend-${var.environment}"
  location = var.region

  template {
    service_account = google_service_account.cloudrun.email

    scaling {
      min_instance_count = var.cloudrun_min_instances
      max_instance_count = var.cloudrun_max_instances
    }

    vpc_access {
      connector = google_vpc_access_connector.connector.id
      egress    = "PRIVATE_RANGES_ONLY"
    }

    volumes {
      name = "cloudsql"
      cloud_sql_instance {
        instances = [google_sql_database_instance.demeter.connection_name]
      }
    }

    containers {
      image = "${var.region}-docker.pkg.dev/${var.project_id}/demeter/backend:latest"

      ports {
        container_port = 8080
      }

      resources {
        limits = {
          cpu    = var.cloudrun_cpu
          memory = var.cloudrun_memory
        }
        cpu_idle          = true
        startup_cpu_boost = true
      }

      # =======================================================================
      # Environment Variables
      # =======================================================================

      env {
        name  = "QUARKUS_PROFILE"
        value = var.environment
      }

      env {
        name  = "CORS_ORIGINS"
        value = var.cors_origins
      }

      # =======================================================================
      # Database Secrets
      # =======================================================================

      env {
        name = "DB_URL"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.db_url.secret_id
            version = "latest"
          }
        }
      }

      env {
        name = "DB_USER"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.db_user.secret_id
            version = "latest"
          }
        }
      }

      env {
        name = "DB_PASSWORD"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.db_password.secret_id
            version = "latest"
          }
        }
      }

      # =======================================================================
      # OIDC Secrets
      # =======================================================================

      env {
        name = "OIDC_AUTH_SERVER_URL"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.oidc_auth_server_url.secret_id
            version = "latest"
          }
        }
      }

      env {
        name = "OIDC_CLIENT_ID"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.oidc_client_id.secret_id
            version = "latest"
          }
        }
      }

      env {
        name = "OIDC_ISSUER"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.oidc_issuer.secret_id
            version = "latest"
          }
        }
      }

      volume_mounts {
        name       = "cloudsql"
        mount_path = "/cloudsql"
      }

      # =======================================================================
      # Health Probes
      # =======================================================================

      startup_probe {
        http_get {
          path = "/q/health/started"
          port = 8080
        }
        initial_delay_seconds = 5
        period_seconds        = 3
        failure_threshold     = 30
        timeout_seconds       = 3
      }

      liveness_probe {
        http_get {
          path = "/q/health/live"
          port = 8080
        }
        period_seconds    = 30
        timeout_seconds   = 3
        failure_threshold = 3
      }
    }
  }

  traffic {
    type    = "TRAFFIC_TARGET_ALLOCATION_TYPE_LATEST"
    percent = 100
  }

  depends_on = [
    google_secret_manager_secret_version.db_url,
    google_secret_manager_secret_version.db_user,
    google_secret_manager_secret_version.db_password,
    google_secret_manager_secret_version.oidc_auth_server_url,
    google_secret_manager_secret_version.oidc_client_id,
    google_secret_manager_secret_version.oidc_issuer,
    google_secret_manager_secret_iam_member.db_url_access,
    google_secret_manager_secret_iam_member.db_user_access,
    google_secret_manager_secret_iam_member.db_password_access,
    google_secret_manager_secret_iam_member.oidc_auth_server_url_access,
    google_secret_manager_secret_iam_member.oidc_client_id_access,
    google_secret_manager_secret_iam_member.oidc_issuer_access,
    google_project_iam_member.cloudrun_cloudsql,
  ]
}

# =============================================================================
# IAM — Allow Unauthenticated Access (Public API)
# =============================================================================

resource "google_cloud_run_v2_service_iam_member" "public" {
  location = google_cloud_run_v2_service.backend.location
  name     = google_cloud_run_v2_service.backend.name
  role     = "roles/run.invoker"
  member   = "allUsers"
}
