package com.fortytwo.demeter.precios.controller;

import com.fortytwo.demeter.common.auth.RoleConstants;
import com.fortytwo.demeter.precios.dto.CreatePriceEntryRequest;
import com.fortytwo.demeter.precios.dto.PriceEntryDTO;
import com.fortytwo.demeter.precios.dto.UpdatePriceEntryRequest;
import com.fortytwo.demeter.precios.service.PriceEntryService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/price-lists/{priceListId}/entries")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PriceEntryController {

    @Inject
    PriceEntryService priceEntryService;

    @GET
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    public List<PriceEntryDTO> listEntries(@PathParam("priceListId") UUID priceListId) {
        return priceEntryService.findByPriceList(priceListId);
    }

    @POST
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR})
    public Response addEntry(
            @PathParam("priceListId") UUID priceListId,
            @Valid CreatePriceEntryRequest request) {
        PriceEntryDTO created = priceEntryService.addEntry(priceListId, request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @POST
    @Path("/bulk")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR})
    public Response bulkAddEntries(
            @PathParam("priceListId") UUID priceListId,
            List<@Valid CreatePriceEntryRequest> requests) {
        List<PriceEntryDTO> created = priceEntryService.bulkAddEntries(priceListId, requests);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{entryId}")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR})
    public PriceEntryDTO updateEntry(
            @PathParam("priceListId") UUID priceListId,
            @PathParam("entryId") UUID entryId,
            @Valid UpdatePriceEntryRequest request) {
        return priceEntryService.updateEntry(entryId, request);
    }

    @DELETE
    @Path("/{entryId}")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR})
    public Response deleteEntry(@PathParam("entryId") UUID entryId) {
        priceEntryService.deleteEntry(entryId);
        return Response.noContent().build();
    }
}
