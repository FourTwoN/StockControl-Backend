package com.fortytwo.demeter.fotos.controller;

import com.fortytwo.demeter.common.auth.RoleConstants;
import com.fortytwo.demeter.common.dto.PagedResponse;
import com.fortytwo.demeter.fotos.dto.CreatePhotoSessionRequest;
import com.fortytwo.demeter.fotos.dto.EstimationDTO;
import com.fortytwo.demeter.fotos.dto.PhotoSessionDTO;
import com.fortytwo.demeter.fotos.dto.ImageDTO;
import com.fortytwo.demeter.fotos.dto.ImageWithUrlsDTO;
import com.fortytwo.demeter.fotos.dto.SessionStatusDTO;
import com.fortytwo.demeter.fotos.service.ImageService;
import com.fortytwo.demeter.fotos.service.PhotoSessionService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/photo-sessions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PhotoSessionController {

    @Inject
    PhotoSessionService photoSessionService;

    @Inject
    ImageService imageService;

    @GET
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    public PagedResponse<PhotoSessionDTO> list(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        return photoSessionService.findAll(page, size);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    public PhotoSessionDTO getById(@PathParam("id") UUID id) {
        return photoSessionService.findById(id);
    }

    @GET
    @Path("/{id}/status")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    public SessionStatusDTO getStatus(@PathParam("id") UUID id) {
        return photoSessionService.getSessionStatus(id);
    }

    @POST
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR})
    public Response create(@Valid CreatePhotoSessionRequest request) {
        PhotoSessionDTO created = photoSessionService.create(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({RoleConstants.ADMIN})
    public Response delete(@PathParam("id") UUID id) {
        photoSessionService.delete(id);
        return Response.noContent().build();
    }

    @GET
    @Path("/{id}/images")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    public List<ImageDTO> getImages(@PathParam("id") UUID id) {
        return imageService.findBySession(id);
    }

    @GET
    @Path("/{id}/estimations")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    public List<EstimationDTO> getEstimations(@PathParam("id") UUID id) {
        return photoSessionService.findEstimationsBySessionId(id);
    }

    /**
     * Get session images with signed URLs for direct browser access.
     *
     * <p>Returns images with time-limited signed URLs that can be used
     * directly in img src attributes. URLs expire after configured duration
     * (default 60 minutes).
     */
    @GET
    @Path("/{id}/images/with-urls")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    public List<ImageWithUrlsDTO> getImagesWithUrls(@PathParam("id") UUID id) {
        return imageService.getSessionImagesWithUrls(id);
    }

    /**
     * Get a single image with signed URL.
     */
    @GET
    @Path("/{sessionId}/images/{imageId}/with-url")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    public ImageWithUrlsDTO getImageWithUrl(
            @PathParam("sessionId") UUID sessionId,
            @PathParam("imageId") UUID imageId) {
        return imageService.getImageWithUrl(imageId);
    }
}
