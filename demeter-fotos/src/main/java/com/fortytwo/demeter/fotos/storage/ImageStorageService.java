package com.fortytwo.demeter.fotos.storage;

import java.time.Duration;
import java.util.Optional;

/**
 * Abstract interface for image storage operations.
 *
 * <p>Implementations can target different storage backends:
 * <ul>
 *   <li>{@code LocalStorageService} - Local filesystem (development)
 *   <li>{@code GcsStorageService} - Google Cloud Storage (production)
 *   <li>{@code S3StorageService} - Amazon S3 (future)
 * </ul>
 *
 * <p>This abstraction allows switching storage providers via configuration
 * without changing application code.
 */
public interface ImageStorageService {

    /**
     * Upload image data to storage.
     *
     * @param data        Image bytes
     * @param path        Storage path (e.g., "sessions/{sessionId}/images/{imageId}.jpg")
     * @param contentType MIME type (e.g., "image/jpeg")
     * @return Storage URL or path that can be used to retrieve the image
     */
    String upload(byte[] data, String path, String contentType);

    /**
     * Generate a signed URL for reading an image.
     *
     * <p>The URL will be valid for the specified duration and can be used
     * directly in browser (e.g., in img src attribute).
     *
     * @param storagePath Path returned from upload()
     * @param expiration  How long the URL should be valid
     * @return Signed URL for direct browser access
     */
    String generateReadUrl(String storagePath, Duration expiration);

    /**
     * Generate a signed URL for uploading directly to storage.
     *
     * <p>Useful for large files where client uploads directly to storage
     * without going through the backend.
     *
     * @param path        Target storage path
     * @param contentType Expected MIME type
     * @param expiration  How long the URL should be valid
     * @return Signed URL for direct upload
     */
    String generateUploadUrl(String path, String contentType, Duration expiration);

    /**
     * Download image data from storage.
     *
     * @param storagePath Path returned from upload()
     * @return Image bytes, or empty if not found
     */
    Optional<byte[]> download(String storagePath);

    /**
     * Delete an image from storage.
     *
     * @param storagePath Path returned from upload()
     * @return true if deleted, false if not found
     */
    boolean delete(String storagePath);

    /**
     * Check if an image exists in storage.
     *
     * @param storagePath Path to check
     * @return true if exists
     */
    boolean exists(String storagePath);

    /**
     * Get the storage provider name (for logging/debugging).
     *
     * @return Provider name (e.g., "local", "gcs", "s3")
     */
    String getProviderName();
}
