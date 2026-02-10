# =============================================================================
# Artifact Registry — Docker Repository
# =============================================================================

resource "google_artifact_registry_repository" "demeter" {
  location      = var.region
  repository_id = "demeter"
  description   = "Docker images for Demeter AI backend"
  format        = "DOCKER"

  cleanup_policies {
    id     = "keep-minimum-versions"
    action = "KEEP"

    most_recent_versions {
      keep_count = 10
    }
  }

  cleanup_policies {
    id     = "delete-old-images"
    action = "DELETE"

    condition {
      older_than = "2592000s" # 30 days
    }
  }
}
