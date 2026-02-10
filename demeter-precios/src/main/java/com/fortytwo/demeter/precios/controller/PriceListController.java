package com.fortytwo.demeter.precios.controller;

import com.fortytwo.demeter.common.auth.RoleConstants;
import com.fortytwo.demeter.common.dto.PagedResponse;
import com.fortytwo.demeter.precios.dto.CreatePriceListRequest;
import com.fortytwo.demeter.precios.dto.PriceListDTO;
import com.fortytwo.demeter.precios.dto.UpdatePriceListRequest;
import com.fortytwo.demeter.precios.service.PriceListService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/price-lists")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PriceListController {

    @Inject
    PriceListService priceListService;

    @GET
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    public PagedResponse<PriceListDTO> list(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("sort") String sort) {
        return priceListService.findAll(page, size, sort);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    public PriceListDTO getById(@PathParam("id") UUID id) {
        return priceListService.findById(id);
    }

    @GET
    @Path("/active")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    public List<PriceListDTO> getActive() {
        return priceListService.findActive();
    }

    @POST
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR})
    public Response create(@Valid CreatePriceListRequest request) {
        PriceListDTO created = priceListService.create(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR})
    public PriceListDTO update(@PathParam("id") UUID id, @Valid UpdatePriceListRequest request) {
        return priceListService.update(id, request);
    }

    @POST
    @Path("/{id}/activate")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR})
    public PriceListDTO activate(@PathParam("id") UUID id) {
        return priceListService.activate(id);
    }

    @POST
    @Path("/{id}/deactivate")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR})
    public PriceListDTO deactivate(@PathParam("id") UUID id) {
        return priceListService.deactivate(id);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({RoleConstants.ADMIN})
    public Response delete(@PathParam("id") UUID id) {
        priceListService.delete(id);
        return Response.noContent().build();
    }
}
