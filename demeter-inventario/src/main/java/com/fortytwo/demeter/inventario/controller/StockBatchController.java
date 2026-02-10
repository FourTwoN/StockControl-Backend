package com.fortytwo.demeter.inventario.controller;

import com.fortytwo.demeter.common.auth.RoleConstants;
import com.fortytwo.demeter.common.dto.PagedResponse;
import com.fortytwo.demeter.inventario.dto.CreateStockBatchRequest;
import com.fortytwo.demeter.inventario.dto.StockBatchDTO;
import com.fortytwo.demeter.inventario.dto.UpdateStockBatchRequest;
import com.fortytwo.demeter.inventario.model.BatchStatus;
import com.fortytwo.demeter.inventario.service.StockBatchService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/stock-batches")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StockBatchController {

    @Inject
    StockBatchService stockBatchService;

    @GET
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    public PagedResponse<StockBatchDTO> list(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("productId") UUID productId,
            @QueryParam("locationId") UUID locationId,
            @QueryParam("status") String status) {
        return stockBatchService.findAll(page, size, productId, locationId, status);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    public StockBatchDTO getById(@PathParam("id") UUID id) {
        return stockBatchService.findById(id);
    }

    @GET
    @Path("/by-product/{productId}")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    public List<StockBatchDTO> getByProduct(@PathParam("productId") UUID productId) {
        return stockBatchService.findByProductId(productId);
    }

    @GET
    @Path("/by-warehouse/{warehouseId}")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    public List<StockBatchDTO> getByWarehouse(@PathParam("warehouseId") UUID warehouseId) {
        return stockBatchService.findByWarehouseId(warehouseId);
    }

    @GET
    @Path("/by-status/{status}")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR, RoleConstants.WORKER, RoleConstants.VIEWER})
    public List<StockBatchDTO> getByStatus(@PathParam("status") BatchStatus status) {
        return stockBatchService.findByStatus(status);
    }

    @POST
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR})
    public Response create(@Valid CreateStockBatchRequest request) {
        StockBatchDTO created = stockBatchService.create(request);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({RoleConstants.ADMIN, RoleConstants.SUPERVISOR})
    public StockBatchDTO update(@PathParam("id") UUID id, @Valid UpdateStockBatchRequest request) {
        return stockBatchService.update(id, request);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({RoleConstants.ADMIN})
    public Response delete(@PathParam("id") UUID id) {
        stockBatchService.delete(id);
        return Response.noContent().build();
    }
}
