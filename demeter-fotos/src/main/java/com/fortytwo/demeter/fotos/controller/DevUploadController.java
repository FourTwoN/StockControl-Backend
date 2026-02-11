package com.fortytwo.demeter.fotos.controller;

import com.fortytwo.demeter.common.tenant.TenantContext;
import com.fortytwo.demeter.fotos.dto.ProcessingResultRequest;
import com.fortytwo.demeter.fotos.dto.SessionStatusDTO;
import com.fortytwo.demeter.fotos.model.Image;
import com.fortytwo.demeter.fotos.model.PhotoProcessingSession;
import com.fortytwo.demeter.fotos.model.ProcessingStatus;
import com.fortytwo.demeter.fotos.repository.ImageRepository;
import com.fortytwo.demeter.fotos.repository.PhotoProcessingSessionRepository;
import com.fortytwo.demeter.fotos.service.ImageService;
import com.fortytwo.demeter.fotos.service.MLWorkerClient;
import com.fortytwo.demeter.fotos.service.MLWorkerClient.DetectionResult;
import com.fortytwo.demeter.fotos.service.MLWorkerClient.MLWorkerResponse;
import com.fortytwo.demeter.fotos.service.ProcessingResultService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Development-only endpoint for direct image upload and ML processing.
 *
 * <p>This controller bypasses Cloud Tasks and calls the ML Worker directly.
 * Use for local development and POC testing only.
 *
 * <p>Flow:
 * 1. Frontend uploads image via Base64 encoded JSON
 * 2. Backend creates session + image record
 * 3. Backend calls ML Worker synchronously
 * 4. Backend persists results
 * 5. Frontend receives results immediately
 */
@Path("/api/v1/dev/ml")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Development ML", description = "Development-only ML processing endpoints")
public class DevUploadController {

    private static final Logger log = Logger.getLogger(DevUploadController.class);

    @Inject
    MLWorkerClient mlWorkerClient;

    @Inject
    PhotoProcessingSessionRepository sessionRepository;

    @Inject
    ImageRepository imageRepository;

    @Inject
    ProcessingResultService processingResultService;

    @Inject
    ImageService imageService;

    @Inject
    TenantContext tenantContext;

    @ConfigProperty(name = "quarkus.profile", defaultValue = "prod")
    String profile;

    /**
     * Request for processing an image via Base64 encoding.
     */
    public record ProcessImageRequest(
            String filename,
            String contentType,
            String imageBase64,
            String pipeline
    ) {}

    @POST
    @Path("/process")
    @Operation(
            summary = "Upload and process image (dev only)",
            description = "Uploads an image (Base64 encoded) and processes it through the ML pipeline synchronously. " +
                    "Creates a session, calls ML Worker, and persists results in one request."
    )
    @APIResponse(
            responseCode = "200",
            description = "Image processed successfully",
            content = @Content(schema = @Schema(implementation = ProcessingResponse.class))
    )
    @APIResponse(responseCode = "400", description = "Invalid request")
    @APIResponse(responseCode = "503", description = "ML Worker not available")
    @Transactional
    public Response processImage(ProcessImageRequest request) {
        // Only allow in dev/test profiles
        if ("prod".equals(profile)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("error", "This endpoint is disabled in production"))
                    .build();
        }

        try {
            // Validate request
            if (request.imageBase64() == null || request.imageBase64().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "imageBase64 is required"))
                        .build();
            }

            String filename = request.filename() != null ? request.filename() : "image.jpg";
            String contentType = request.contentType() != null ? request.contentType() : "image/jpeg";
            String pipeline = request.pipeline() != null ? request.pipeline() : "SEGMENT_DETECT";

            // Decode Base64 image
            byte[] imageData;
            try {
                // Remove data URL prefix if present (e.g., "data:image/jpeg;base64,")
                String base64Data = request.imageBase64();
                if (base64Data.contains(",")) {
                    base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
                }
                imageData = java.util.Base64.getDecoder().decode(base64Data);
            } catch (Exception e) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Invalid Base64 image data: " + e.getMessage()))
                        .build();
            }

            log.infof("Processing image: %s (%s, %d bytes, pipeline=%s)",
                    filename, contentType, imageData.length, pipeline);

            // Validate content type
            if (!isValidImageType(contentType)) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Invalid content type: " + contentType +
                                ". Allowed: image/jpeg, image/png, image/webp"))
                        .build();
            }

            // Check ML Worker availability
            if (!mlWorkerClient.isAvailable()) {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity(Map.of("error", "ML Worker is not available. Make sure it's running on " +
                                "http://localhost:8000"))
                        .build();
            }

            // Create session
            PhotoProcessingSession session = new PhotoProcessingSession();
            session.setStatus(ProcessingStatus.PROCESSING);
            session.setTotalImages(1);
            session.setProcessedImages(0);
            sessionRepository.persist(session);

            log.infof("Created session: %s", session.getId());

            // Upload image to storage
            String storagePath = imageService.uploadAndStoreImage(
                    session.getId(), imageData, filename, contentType
            );

            log.infof("Uploaded image to storage: %s", storagePath);

            // Create image record
            Image image = new Image();
            image.setSession(session);
            image.setStorageUrl(storagePath);
            image.setOriginalFilename(filename);
            image.setFileSize((long) imageData.length);
            image.setMimeType(contentType);
            imageRepository.persist(image);

            log.infof("Created image: %s", image.getId());

            // Call ML Worker
            MLWorkerResponse mlResponse = mlWorkerClient.processImage(
                    imageData, filename, contentType, pipeline
            );

            if (!mlResponse.success) {
                session.setStatus(ProcessingStatus.FAILED);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(Map.of(
                                "error", "ML processing failed: " + mlResponse.error,
                                "sessionId", session.getId().toString()
                        ))
                        .build();
            }

            // Convert ML Worker response to ProcessingResultRequest and persist
            ProcessingResultRequest resultRequest = convertToResultRequest(
                    session.getId(), image.getId(), mlResponse
            );
            SessionStatusDTO status = processingResultService.processResults(resultRequest);

            // Build response
            ProcessingResponse response = new ProcessingResponse(
                    true,
                    session.getId(),
                    image.getId(),
                    pipeline,
                    mlResponse.durationMs,
                    mlResponse.results != null && mlResponse.results.segmentation != null
                            ? mlResponse.results.segmentation.size() : 0,
                    mlResponse.results != null && mlResponse.results.detection != null
                            ? mlResponse.results.detection.size() : 0,
                    status,
                    null
            );

            return Response.ok(response).build();

        } catch (Exception e) {
            log.error("Processing failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Processing failed: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/pipelines")
    @Operation(summary = "List available ML pipelines")
    public Response getPipelines() {
        if ("prod".equals(profile)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("error", "This endpoint is disabled in production"))
                    .build();
        }

        try {
            return Response.ok(mlWorkerClient.getPipelines()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "ML Worker not available: " + e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/health")
    @Operation(summary = "Check ML Worker health")
    public Response checkHealth() {
        boolean available = mlWorkerClient.isAvailable();
        return Response.ok(Map.of(
                "mlWorkerAvailable", available,
                "profile", profile
        )).build();
    }

    private ProcessingResultRequest convertToResultRequest(
            UUID sessionId, UUID imageId, MLWorkerResponse mlResponse
    ) {
        List<ProcessingResultRequest.DetectionResultItem> detections = new ArrayList<>();

        if (mlResponse.results != null && mlResponse.results.detection != null) {
            for (DetectionResult d : mlResponse.results.detection) {
                // Convert center + width/height to x1,y1,x2,y2
                double x1 = d.centerXPx - d.widthPx / 2;
                double y1 = d.centerYPx - d.heightPx / 2;
                double x2 = d.centerXPx + d.widthPx / 2;
                double y2 = d.centerYPx + d.heightPx / 2;

                detections.add(new ProcessingResultRequest.DetectionResultItem(
                        d.className,
                        d.confidence,
                        new ProcessingResultRequest.BoundingBox(x1, y1, x2, y2)
                ));
            }
        }

        return new ProcessingResultRequest(
                sessionId,
                imageId,
                detections,
                null, // classifications
                null, // estimations
                new ProcessingResultRequest.ProcessingMetadata(
                        mlResponse.pipeline,
                        (long) mlResponse.durationMs,
                        null,
                        "dev"
                )
        );
    }

    private boolean isValidImageType(String contentType) {
        return contentType != null && (
                contentType.contains("image/jpeg") ||
                contentType.contains("image/jpg") ||
                contentType.contains("image/png") ||
                contentType.contains("image/webp")
        );
    }

    /**
     * Response from the dev ML processing endpoint.
     */
    public record ProcessingResponse(
            boolean success,
            UUID sessionId,
            UUID imageId,
            String pipeline,
            int durationMs,
            int segmentsFound,
            int detectionsFound,
            SessionStatusDTO sessionStatus,
            String error
    ) {}
}
