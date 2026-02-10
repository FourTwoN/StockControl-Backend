package com.fortytwo.demeter.inventario.controller;

import com.fortytwo.demeter.common.auth.RoleConstants;
import com.fortytwo.demeter.common.dto.PagedResponse;
import com.fortytwo.demeter.inventario.dto.CreateStockMovementRequest;
import com.fortytwo.demeter.inventario.dto.StockMovementDTO;
import com.fortytwo.demeter.inventario.model.MovementType;
import com.fortytwo.demeter.inventario.service.StockMovementService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/stock-movements")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StockMovementController {

    @Inject
    StockMovementService stockMovementService;

    @GET
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    public PagedResponse<StockMovementDTO> list(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("batchId") UUID batchId,
            @QueryParam("type") String type,
            @QueryParam("startDate") Instant startDate,
            @QueryParam("endDate") Instant endDate) {
        return stockMovementService.findAll(page, size, batchId, type, startDate, endDate);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    public StockMovementDTO getById(@PathParam("id") UUID id) {
        return stockMovementService.findById(id);
    }

    @GET
    @Path("/by-type/{type}")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    public List<StockMovementDTO> getByType(@PathParam("type") MovementType type) {
        return stockMovementService.findByMovementType(type);
    }

    @GET
    @Path("/by-date-range")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    public List<StockMovementDTO> getByDateRange(
            @QueryParam("from") Instant from,
            @QueryParam("to") Instant to) {
        return stockMovementService.findByDateRange(from, to);
    }

    @GET
    @Path("/by-reference/{referenceId}")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    public List<StockMovementDTO> getByReference(@PathParam("referenceId") UUID referenceId) {
        return stockMovementService.findByReferenceId(referenceId);
    }

    @POST
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR})
    public Response create(@Valid CreateStockMovementRequest request) {
        StockMovementDTO created = stockMovementService.create(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }
}
