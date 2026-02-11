package com.fortytwo.demeter.fotos.controller;

import com.fortytwo.demeter.fotos.storage.LocalStorageService;
import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.Optional;

/**
 * Serves locally stored files in development mode.
 *
 * <p>This controller is only active in dev profile and serves files
 * that were stored by {@link LocalStorageService}.
 *
 * <p>In production, files are served directly from GCS via signed URLs.
 */
@Path("/api/v1/storage")
@Tag(name = "Storage (Dev)", description = "Local file serving for development")
@IfBuildProfile("dev")
public class LocalStorageController {

    private static final Logger log = Logger.getLogger(LocalStorageController.class);

    @Inject
    LocalStorageService storageService;

    @GET
    @Path("/{path:.+}")
    @Operation(summary = "Serve a locally stored file (dev only)")
    public Response serveFile(@PathParam("path") String path) {
        log.debugf("Serving local file: %s", path);

        Optional<byte[]> data = storageService.download(path);

        if (data.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("File not found: " + path)
                    .build();
        }

        // Detect content type from extension
        String contentType = detectContentType(path);

        return Response.ok(data.get())
                .type(contentType)
                .header("Cache-Control", "public, max-age=3600")
                .build();
    }

    private String detectContentType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lower.endsWith(".png")) {
            return "image/png";
        } else if (lower.endsWith(".webp")) {
            return "image/webp";
        } else if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
