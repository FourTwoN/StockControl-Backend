package com.fortytwo.demeter.fotos.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Image DTO with signed URLs for direct browser access.
 *
 * <p>This DTO is used when returning images to the frontend,
 * replacing internal storage paths with time-limited signed URLs.
 */
public record ImageWithUrlsDTO(
        UUID id,
        UUID sessionId,
        String originalFilename,
        Long fileSize,
        String mimeType,

        // Signed URLs for direct browser access
        String imageUrl,
        String thumbnailUrl,

        // Processing results
        List<DetectionDTO> detections,
        List<ClassificationDTO> classifications,

        // Timestamps
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * Builder for creating ImageWithUrlsDTO.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID sessionId;
        private String originalFilename;
        private Long fileSize;
        private String mimeType;
        private String imageUrl;
        private String thumbnailUrl;
        private List<DetectionDTO> detections = List.of();
        private List<ClassificationDTO> classifications = List.of();
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder sessionId(UUID sessionId) { this.sessionId = sessionId; return this; }
        public Builder originalFilename(String originalFilename) { this.originalFilename = originalFilename; return this; }
        public Builder fileSize(Long fileSize) { this.fileSize = fileSize; return this; }
        public Builder mimeType(String mimeType) { this.mimeType = mimeType; return this; }
        public Builder imageUrl(String imageUrl) { this.imageUrl = imageUrl; return this; }
        public Builder thumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; return this; }
        public Builder detections(List<DetectionDTO> detections) { this.detections = detections; return this; }
        public Builder classifications(List<ClassificationDTO> classifications) { this.classifications = classifications; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public ImageWithUrlsDTO build() {
            return new ImageWithUrlsDTO(
                    id, sessionId, originalFilename, fileSize, mimeType,
                    imageUrl, thumbnailUrl,
                    detections, classifications,
                    createdAt, updatedAt
            );
        }
    }
}
