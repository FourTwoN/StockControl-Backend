package com.fortytwo.demeter.costos.controller;

import com.fortytwo.demeter.common.auth.RoleConstants;
import com.fortytwo.demeter.common.dto.PagedResponse;
import com.fortytwo.demeter.costos.dto.CostDTO;
import com.fortytwo.demeter.costos.dto.CostTrendDTO;
import com.fortytwo.demeter.costos.dto.CreateCostRequest;
import com.fortytwo.demeter.costos.dto.InventoryValuationDTO;
import com.fortytwo.demeter.costos.dto.ProductCostDTO;
import com.fortytwo.demeter.costos.dto.UpdateCostRequest;
import com.fortytwo.demeter.costos.service.CostService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/costs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CostController {

    @Inject
    CostService costService;

    @GET
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    public PagedResponse<CostDTO> list(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        return costService.findAll(page, size);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    public CostDTO getById(@PathParam("id") UUID id) {
        return costService.findById(id);
    }

    @GET
    @Path("/by-product/{productId}")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    public List<CostDTO> getByProduct(@PathParam("productId") UUID productId) {
        return costService.findByProduct(productId);
    }

    @GET
    @Path("/by-batch/{batchId}")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    public List<CostDTO> getByBatch(@PathParam("batchId") UUID batchId) {
        return costService.findByBatch(batchId);
    }

    @GET
    @Path("/products")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.VIEWER})
    public PagedResponse<ProductCostDTO> getProductCosts(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        return costService.getProductCosts(page, size);
    }

    @GET
    @Path("/valuation")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.VIEWER})
    public InventoryValuationDTO getValuation() {
        return costService.getValuation();
    }

    @GET
    @Path("/trends")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.VIEWER})
    public List<CostTrendDTO> getTrends(
            @QueryParam("productId") UUID productId,
            @QueryParam("from") String from,
            @QueryParam("to") String to) {
        LocalDate fromDate = from != null ? LocalDate.parse(from) : null;
        LocalDate toDate = to != null ? LocalDate.parse(to) : null;
        return costService.getTrends(productId, fromDate, toDate);
    }

    @POST
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR})
    public Response create(@Valid CreateCostRequest request) {
        CostDTO created = costService.create(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR})
    public CostDTO update(@PathParam("id") UUID id, @Valid UpdateCostRequest request) {
        return costService.update(id, request);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({RoleConstants.ADMIN})
    public Response delete(@PathParam("id") UUID id) {
        costService.delete(id);
        return Response.noContent().build();
    }
}
