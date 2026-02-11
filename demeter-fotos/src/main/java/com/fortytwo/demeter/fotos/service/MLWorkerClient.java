package com.fortytwo.demeter.fotos.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * HTTP client for calling the ML Worker service directly.
 *
 * <p>Used in development mode when Cloud Tasks is disabled.
 * Makes synchronous HTTP calls to the ML Worker's /dev/process-image endpoint.
 */
@ApplicationScoped
public class MLWorkerClient {

    private static final Logger log = Logger.getLogger(MLWorkerClient.class);

    @ConfigProperty(name = "demeter.mlworker.url", defaultValue = "http://localhost:8000")
    String mlWorkerUrl;

    @ConfigProperty(name = "demeter.mlworker.timeout-seconds", defaultValue = "300")
    int timeoutSeconds;

    @Inject
    ObjectMapper objectMapper;

    private HttpClient httpClient;

    /**
     * Process an image through the ML Worker pipeline.
     *
     * <p>Uses JSON endpoint with Base64-encoded image to avoid multipart complexity.
     *
     * @param imageData Image bytes to process
     * @param filename Original filename
     * @param contentType MIME type (e.g., image/jpeg)
     * @param pipeline Pipeline name (e.g., SEGMENT_DETECT)
     * @return Processing result from ML Worker
     */
    public MLWorkerResponse processImage(byte[] imageData, String filename, String contentType, String pipeline) {
        log.infof("Calling ML Worker: url=%s, pipeline=%s, filename=%s, size=%d bytes",
                mlWorkerUrl, pipeline, filename, imageData.length);

        try {
            // Encode image as Base64
            String imageBase64 = java.util.Base64.getEncoder().encodeToString(imageData);

            // Build JSON request body
            String jsonBody = objectMapper.writeValueAsString(Map.of(
                    "filename", filename,
                    "contentType", contentType,
                    "imageBase64", imageBase64,
                    "pipeline", pipeline
            ));

            String targetUrl = mlWorkerUrl + "/dev/process-image-json";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            log.debugf("Sending request to ML Worker: url=%s, jsonLength=%d", targetUrl, jsonBody.length());

            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                MLWorkerResponse result = objectMapper.readValue(response.body(), MLWorkerResponse.class);
                log.infof("ML Worker response: success=%s, pipeline=%s, duration=%dms, detections=%d",
                        result.success, result.pipeline, result.durationMs,
                        result.results != null && result.results.detection != null ? result.results.detection.size() : 0);
                return result;
            } else {
                log.errorf("ML Worker error: status=%d, body=%s", response.statusCode(), response.body());
                throw new RuntimeException("ML Worker returned status " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception e) {
            log.error("Failed to call ML Worker", e);
            throw new RuntimeException("ML Worker call failed: " + e.getMessage(), e);
        }
    }

    /**
     * Check if ML Worker is available.
     */
    public boolean isAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(mlWorkerUrl + "/health"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            log.warn("ML Worker health check failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get available pipelines from ML Worker.
     */
    public PipelinesResponse getPipelines() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(mlWorkerUrl + "/dev/pipelines"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), PipelinesResponse.class);
            }
            throw new RuntimeException("Failed to get pipelines: " + response.statusCode());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get pipelines: " + e.getMessage(), e);
        }
    }

    private synchronized HttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .version(HttpClient.Version.HTTP_1_1)  // Uvicorn only supports HTTP/1.1
                    .build();
        }
        return httpClient;
    }

    // DTO classes for ML Worker responses

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MLWorkerResponse {
        public boolean success;
        @JsonProperty("image_id")
        public String imageId;
        public String pipeline;
        @JsonProperty("duration_ms")
        public int durationMs;
        @JsonProperty("steps_completed")
        public int stepsCompleted;
        public PipelineResults results;
        public String error;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PipelineResults {
        @JsonProperty("pipeline_name")
        public String pipelineName;
        public boolean success;
        @JsonProperty("total_duration_ms")
        public int totalDurationMs;
        public List<SegmentationResult> segmentation;
        public List<DetectionResult> detection;
        public String error;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SegmentationResult {
        @JsonProperty("segment_idx")
        public int segmentIdx;
        @JsonProperty("class_name")
        public String className;
        public double confidence;
        public List<Double> bbox;
        @JsonProperty("area_px")
        public double areaPx;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DetectionResult {
        @JsonProperty("center_x_px")
        public double centerXPx;
        @JsonProperty("center_y_px")
        public double centerYPx;
        @JsonProperty("width_px")
        public double widthPx;
        @JsonProperty("height_px")
        public double heightPx;
        public double confidence;
        @JsonProperty("class_name")
        public String className;
        @JsonProperty("segment_idx")
        public int segmentIdx;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PipelinesResponse {
        public String industry;
        public String version;
        public Map<String, PipelineInfo> pipelines;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PipelineInfo {
        public List<String> steps;
    }
}
