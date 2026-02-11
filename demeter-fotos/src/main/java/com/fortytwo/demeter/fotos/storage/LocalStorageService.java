package com.fortytwo.demeter.fotos.storage;

import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

/**
 * Local filesystem storage implementation for development.
 *
 * <p>Stores images in a local directory and serves them via a simple
 * file:// URL or a configurable base URL for local dev server.
 *
 * <p>This implementation is only active in the "dev" profile.
 */
@ApplicationScoped
@IfBuildProfile("dev")
public class LocalStorageService implements ImageStorageService {

    private static final Logger log = Logger.getLogger(LocalStorageService.class);

    @ConfigProperty(name = "demeter.storage.local.base-path", defaultValue = "/tmp/demeter-storage")
    String basePath;

    @ConfigProperty(name = "demeter.storage.local.base-url", defaultValue = "http://localhost:8080/api/v1/storage")
    String baseUrl;

    private Path storageRoot;

    @PostConstruct
    void init() {
        storageRoot = Path.of(basePath);
        try {
            Files.createDirectories(storageRoot);
            log.infof("Local storage initialized at: %s", storageRoot.toAbsolutePath());
        } catch (IOException e) {
            log.errorf("Failed to create storage directory: %s", e.getMessage());
            throw new RuntimeException("Cannot initialize local storage", e);
        }
    }

    @Override
    public String upload(byte[] data, String path, String contentType) {
        Path targetPath = storageRoot.resolve(path);

        try {
            // Create parent directories if needed
            Files.createDirectories(targetPath.getParent());

            // Write file
            Files.write(targetPath, data);

            log.infof("Stored file locally: %s (%d bytes)", path, data.length);

            // Return the storage path (not the full filesystem path)
            return path;

        } catch (IOException e) {
            log.errorf("Failed to store file: %s - %s", path, e.getMessage());
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateReadUrl(String storagePath, Duration expiration) {
        // For local dev, we serve files through a backend endpoint
        // The "signature" is just a dummy param for dev
        String url = baseUrl + "/" + storagePath + "?expires=" + System.currentTimeMillis() + expiration.toMillis();
        log.debugf("Generated local read URL: %s", url);
        return url;
    }

    @Override
    public String generateUploadUrl(String path, String contentType, Duration expiration) {
        // For local dev, direct upload isn't really needed
        // Return a URL that would accept PUT requests
        return baseUrl + "/upload/" + path + "?expires=" + System.currentTimeMillis() + expiration.toMillis();
    }

    @Override
    public Optional<byte[]> download(String storagePath) {
        Path filePath = storageRoot.resolve(storagePath);

        if (!Files.exists(filePath)) {
            log.warnf("File not found: %s", storagePath);
            return Optional.empty();
        }

        try {
            byte[] data = Files.readAllBytes(filePath);
            log.debugf("Downloaded file: %s (%d bytes)", storagePath, data.length);
            return Optional.of(data);
        } catch (IOException e) {
            log.errorf("Failed to read file: %s - %s", storagePath, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public boolean delete(String storagePath) {
        Path filePath = storageRoot.resolve(storagePath);

        try {
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.infof("Deleted file: %s", storagePath);
            }
            return deleted;
        } catch (IOException e) {
            log.errorf("Failed to delete file: %s - %s", storagePath, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean exists(String storagePath) {
        return Files.exists(storageRoot.resolve(storagePath));
    }

    @Override
    public String getProviderName() {
        return "local";
    }

    /**
     * Get the full filesystem path for a storage path.
     * Used by the local file serving endpoint.
     */
    public Path getFullPath(String storagePath) {
        return storageRoot.resolve(storagePath);
    }
}
