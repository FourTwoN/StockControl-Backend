package com.fortytwo.demeter.fotos.storage;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import io.quarkus.arc.profile.UnlessBuildProfile;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Google Cloud Storage implementation for production.
 *
 * <p>Uses GCS SDK to store images and generate signed URLs for secure access.
 *
 * <p>This implementation is active in all profiles except "dev".
 *
 * <p>Required configuration:
 * <ul>
 *   <li>{@code demeter.storage.gcs.bucket} - GCS bucket name
 *   <li>{@code demeter.storage.gcs.project-id} - GCP project ID
 *   <li>{@code GOOGLE_APPLICATION_CREDENTIALS} - Path to service account JSON (or use workload identity)
 * </ul>
 */
@ApplicationScoped
@UnlessBuildProfile("dev")
public class GcsStorageService implements ImageStorageService {

    private static final Logger log = Logger.getLogger(GcsStorageService.class);

    @ConfigProperty(name = "demeter.storage.gcs.bucket")
    String bucketName;

    @ConfigProperty(name = "demeter.storage.gcs.project-id")
    String projectId;

    @ConfigProperty(name = "demeter.storage.gcs.base-path", defaultValue = "images")
    String basePath;

    private Storage storage;

    @PostConstruct
    void init() {
        try {
            storage = StorageOptions.newBuilder()
                    .setProjectId(projectId)
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .build()
                    .getService();

            log.infof("GCS storage initialized: bucket=%s, project=%s", bucketName, projectId);

            // Verify bucket exists
            Bucket bucket = storage.get(bucketName);
            if (bucket == null) {
                log.warnf("Bucket does not exist: %s (will be created on first upload)", bucketName);
            }

        } catch (IOException e) {
            log.errorf("Failed to initialize GCS storage: %s", e.getMessage());
            throw new RuntimeException("Cannot initialize GCS storage", e);
        }
    }

    @Override
    public String upload(byte[] data, String path, String contentType) {
        String fullPath = basePath + "/" + path;

        BlobId blobId = BlobId.of(bucketName, fullPath);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();

        try {
            Blob blob = storage.create(blobInfo, data);
            log.infof("Uploaded to GCS: gs://%s/%s (%d bytes)", bucketName, fullPath, data.length);
            return fullPath;

        } catch (StorageException e) {
            log.errorf("Failed to upload to GCS: %s - %s", fullPath, e.getMessage());
            throw new RuntimeException("Failed to upload to GCS: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateReadUrl(String storagePath, Duration expiration) {
        BlobId blobId = BlobId.of(bucketName, storagePath);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

        try {
            URL signedUrl = storage.signUrl(
                    blobInfo,
                    expiration.toMinutes(),
                    TimeUnit.MINUTES,
                    Storage.SignUrlOption.withV4Signature()
            );

            log.debugf("Generated signed read URL for: %s (expires in %d min)",
                    storagePath, expiration.toMinutes());

            return signedUrl.toString();

        } catch (StorageException e) {
            log.errorf("Failed to generate signed URL: %s - %s", storagePath, e.getMessage());
            throw new RuntimeException("Failed to generate signed URL: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateUploadUrl(String path, String contentType, Duration expiration) {
        String fullPath = basePath + "/" + path;

        BlobId blobId = BlobId.of(bucketName, fullPath);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();

        try {
            URL signedUrl = storage.signUrl(
                    blobInfo,
                    expiration.toMinutes(),
                    TimeUnit.MINUTES,
                    Storage.SignUrlOption.withV4Signature(),
                    Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                    Storage.SignUrlOption.withContentType()
            );

            log.debugf("Generated signed upload URL for: %s (expires in %d min)",
                    fullPath, expiration.toMinutes());

            return signedUrl.toString();

        } catch (StorageException e) {
            log.errorf("Failed to generate upload URL: %s - %s", fullPath, e.getMessage());
            throw new RuntimeException("Failed to generate upload URL: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<byte[]> download(String storagePath) {
        BlobId blobId = BlobId.of(bucketName, storagePath);

        try {
            Blob blob = storage.get(blobId);
            if (blob == null || !blob.exists()) {
                log.warnf("Blob not found: gs://%s/%s", bucketName, storagePath);
                return Optional.empty();
            }

            byte[] data = blob.getContent();
            log.debugf("Downloaded from GCS: %s (%d bytes)", storagePath, data.length);
            return Optional.of(data);

        } catch (StorageException e) {
            log.errorf("Failed to download from GCS: %s - %s", storagePath, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean delete(String storagePath) {
        BlobId blobId = BlobId.of(bucketName, storagePath);

        try {
            boolean deleted = storage.delete(blobId);
            if (deleted) {
                log.infof("Deleted from GCS: gs://%s/%s", bucketName, storagePath);
            }
            return deleted;

        } catch (StorageException e) {
            log.errorf("Failed to delete from GCS: %s - %s", storagePath, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean exists(String storagePath) {
        BlobId blobId = BlobId.of(bucketName, storagePath);
        Blob blob = storage.get(blobId);
        return blob != null && blob.exists();
    }

    @Override
    public String getProviderName() {
        return "gcs";
    }
}
