package com.fortytwo.demeter.fotos.service;

import com.fortytwo.demeter.common.cloudtasks.CloudTasksService;
import com.fortytwo.demeter.common.cloudtasks.ProcessingTaskRequest;
import com.fortytwo.demeter.common.exception.EntityNotFoundException;
import com.fortytwo.demeter.common.tenant.TenantContext;
import com.fortytwo.demeter.fotos.dto.ClassificationDTO;
import com.fortytwo.demeter.fotos.dto.CreateImageRequest;
import com.fortytwo.demeter.fotos.dto.DetectionDTO;
import com.fortytwo.demeter.fotos.dto.ImageDTO;
import com.fortytwo.demeter.fotos.dto.ImageWithUrlsDTO;
import com.fortytwo.demeter.fotos.model.Image;
import com.fortytwo.demeter.fotos.model.PhotoProcessingSession;
import com.fortytwo.demeter.fotos.repository.ClassificationRepository;
import com.fortytwo.demeter.fotos.repository.DetectionRepository;
import com.fortytwo.demeter.fotos.repository.ImageRepository;
import com.fortytwo.demeter.fotos.repository.PhotoProcessingSessionRepository;
import com.fortytwo.demeter.fotos.storage.ImageStorageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ImageService {

    private static final Logger log = Logger.getLogger(ImageService.class);

    @Inject
    ImageRepository imageRepository;

    @Inject
    PhotoProcessingSessionRepository sessionRepository;

    @Inject
    DetectionRepository detectionRepository;

    @Inject
    ClassificationRepository classificationRepository;

    @Inject
    CloudTasksService cloudTasksService;

    @Inject
    TenantContext tenantContext;

    @Inject
    ImageStorageService storageService;

    @ConfigProperty(name = "demeter.storage.url-expiration-minutes", defaultValue = "60")
    int urlExpirationMinutes;

    @Transactional
    public ImageDTO addImage(UUID sessionId, CreateImageRequest request) {
        return addImage(sessionId, request, "DETECTION");
    }

    @Transactional
    public ImageDTO addImage(UUID sessionId, CreateImageRequest request, String pipeline) {
        PhotoProcessingSession session = sessionRepository.findByIdOptional(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("PhotoProcessingSession", sessionId));

        Image image = new Image();
        image.setSession(session);
        image.setStorageUrl(request.storageUrl());
        image.setThumbnailUrl(request.thumbnailUrl());
        image.setOriginalFilename(request.originalFilename());
        image.setFileSize(request.fileSize());
        image.setMimeType(request.mimeType());

        imageRepository.persist(image);
        session.setTotalImages(session.getTotalImages() + 1);

        log.infof("Added image %s to session %s", image.getId(), sessionId);

        // Dispatch ML processing task
        dispatchProcessingTask(session, image, pipeline);

        return ImageDTO.from(image);
    }

    /**
     * Dispatch an ML processing task via Cloud Tasks.
     *
     * <p>Creates a Cloud Task that will invoke the ML Worker to process
     * the image. The task includes tenant isolation via tenant_id.
     */
    private void dispatchProcessingTask(PhotoProcessingSession session, Image image, String pipeline) {
        String tenantId = tenantContext.getCurrentTenantId();

        ProcessingTaskRequest taskRequest = ProcessingTaskRequest.of(
                tenantId,
                session.getId(),
                image.getId(),
                image.getStorageUrl(),
                pipeline
        );

        String taskName = cloudTasksService.createProcessingTask(taskRequest);

        if (taskName != null) {
            log.infof("Dispatched ML processing task: %s for image %s", taskName, image.getId());
        }
    }

    public List<ImageDTO> findBySession(UUID sessionId) {
        sessionRepository.findByIdOptional(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("PhotoProcessingSession", sessionId));
        return imageRepository.findBySessionId(sessionId)
                .stream().map(ImageDTO::from).toList();
    }

    public ImageDTO findById(UUID id) {
        Image image = imageRepository.findByIdOptional(id)
                .orElseThrow(() -> new EntityNotFoundException("Image", id));
        return ImageDTO.from(image);
    }

    public List<DetectionDTO> findDetectionsByImageId(UUID imageId) {
        imageRepository.findByIdOptional(imageId)
                .orElseThrow(() -> new EntityNotFoundException("Image", imageId));
        return detectionRepository.findByImageId(imageId)
                .stream().map(DetectionDTO::from).toList();
    }

    public List<ClassificationDTO> findClassificationsByImageId(UUID imageId) {
        imageRepository.findByIdOptional(imageId)
                .orElseThrow(() -> new EntityNotFoundException("Image", imageId));
        return classificationRepository.findByImageId(imageId)
                .stream().map(ClassificationDTO::from).toList();
    }

    // =========================================================================
    // Methods with Signed URLs
    // =========================================================================

    /**
     * Get all images for a session with signed URLs.
     *
     * @param sessionId Session ID
     * @return List of images with signed URLs and detections
     */
    public List<ImageWithUrlsDTO> getSessionImagesWithUrls(UUID sessionId) {
        sessionRepository.findByIdOptional(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("PhotoProcessingSession", sessionId));

        List<Image> images = imageRepository.findBySessionId(sessionId);

        log.infof("Converting %d images for session %s with signed URLs", images.size(), sessionId);

        return images.stream()
                .map(this::toImageWithUrls)
                .toList();
    }

    /**
     * Get a single image with signed URL.
     *
     * @param imageId Image ID
     * @return Image with signed URL
     */
    public ImageWithUrlsDTO getImageWithUrl(UUID imageId) {
        Image image = imageRepository.findByIdOptional(imageId)
                .orElseThrow(() -> new EntityNotFoundException("Image", imageId));
        return toImageWithUrls(image);
    }

    /**
     * Upload an image to storage and create the database record.
     *
     * @param sessionId   Session to attach the image to
     * @param data        Image bytes
     * @param filename    Original filename
     * @param contentType MIME type
     * @return Storage path that was used
     */
    @Transactional
    public String uploadAndStoreImage(UUID sessionId, byte[] data, String filename, String contentType) {
        // Generate storage path
        String storagePath = generateStoragePath(sessionId, filename);

        // Upload to storage
        String actualPath = storageService.upload(data, storagePath, contentType);

        log.infof("Uploaded image to storage: %s (%d bytes, provider=%s)",
                actualPath, data.length, storageService.getProviderName());

        return actualPath;
    }

    /**
     * Convert Image entity to DTO with signed URLs.
     */
    private ImageWithUrlsDTO toImageWithUrls(Image image) {
        Duration expiration = Duration.ofMinutes(urlExpirationMinutes);

        // Generate signed URL for the image
        String imageUrl = null;
        if (image.getStorageUrl() != null && !image.getStorageUrl().isEmpty()) {
            try {
                imageUrl = storageService.generateReadUrl(image.getStorageUrl(), expiration);
            } catch (Exception e) {
                log.warnf("Failed to generate signed URL for image %s: %s", image.getId(), e.getMessage());
                // Fallback to raw URL for dev compatibility
                imageUrl = image.getStorageUrl();
            }
        }

        // Generate signed URL for thumbnail (if exists)
        String thumbnailUrl = null;
        if (image.getThumbnailUrl() != null && !image.getThumbnailUrl().isEmpty()) {
            try {
                thumbnailUrl = storageService.generateReadUrl(image.getThumbnailUrl(), expiration);
            } catch (Exception e) {
                log.warnf("Failed to generate signed URL for thumbnail %s: %s", image.getId(), e.getMessage());
            }
        }

        // Convert detections
        List<DetectionDTO> detections = image.getDetections() != null
                ? image.getDetections().stream().map(DetectionDTO::from).toList()
                : List.of();

        // Convert classifications
        List<ClassificationDTO> classifications = image.getClassifications() != null
                ? image.getClassifications().stream().map(ClassificationDTO::from).toList()
                : List.of();

        return ImageWithUrlsDTO.builder()
                .id(image.getId())
                .sessionId(image.getSession() != null ? image.getSession().getId() : null)
                .originalFilename(image.getOriginalFilename())
                .fileSize(image.getFileSize())
                .mimeType(image.getMimeType())
                .imageUrl(imageUrl)
                .thumbnailUrl(thumbnailUrl)
                .detections(detections)
                .classifications(classifications)
                .createdAt(image.getCreatedAt())
                .updatedAt(image.getUpdatedAt())
                .build();
    }

    /**
     * Generate a unique storage path for an image.
     */
    private String generateStoragePath(UUID sessionId, String filename) {
        String extension = getFileExtension(filename);
        String uniqueFilename = UUID.randomUUID() + extension;
        return String.format("sessions/%s/images/%s", sessionId, uniqueFilename);
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(dotIndex) : ".jpg";
    }
}
