# =============================================================================
# Cloud SQL — PostgreSQL 17
# =============================================================================

resource "google_sql_database_instance" "demeter" {
  name             = "demeter-db-${var.environment}"
  database_version = "POSTGRES_17"
  region           = var.region

  settings {
    tier              = var.db_tier
    availability_type = var.environment == "prod" ? "REGIONAL" : "ZONAL"
    disk_size         = var.db_disk_size
    disk_type         = "PD_SSD"
    disk_autoresize   = true

    database_flags {
      name  = "max_connections"
      value = tostring(var.db_max_connections)
    }

    # Enable pgvector for future AI features
    database_flags {
      name  = "cloudsql.enable_pg_vector"
      value = "on"
    }

    backup_configuration {
      enabled                        = true
      point_in_time_recovery_enabled = var.environment == "prod"
      start_time                     = "03:00"
      transaction_log_retention_days = var.environment == "prod" ? 7 : 1

      backup_retention_settings {
        retained_backups = var.environment == "prod" ? 30 : 7
      }
    }

    ip_configuration {
      ipv4_enabled    = false
      private_network = google_compute_network.vpc.id
    }

    maintenance_window {
      day          = 7 # Sunday
      hour         = 4 # 4 AM UTC
      update_track = "stable"
    }

    insights_config {
      query_insights_enabled  = true
      query_plans_per_minute  = 5
      query_string_length     = 1024
      record_application_tags = true
      record_client_address   = false
    }
  }

  deletion_protection = var.environment == "prod"

  depends_on = [google_service_networking_connection.private_vpc]
}

# =============================================================================
# Database
# =============================================================================

resource "google_sql_database" "demeter" {
  name     = "demeter"
  instance = google_sql_database_instance.demeter.name
}

# =============================================================================
# Database User
# =============================================================================

resource "google_sql_user" "demeter_app" {
  name     = "demeter_app"
  instance = google_sql_database_instance.demeter.name
  password = var.db_password
}
