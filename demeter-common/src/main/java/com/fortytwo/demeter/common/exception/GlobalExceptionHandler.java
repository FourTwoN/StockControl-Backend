package com.fortytwo.demeter.common.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionHandler.class);

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof EntityNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse(404, "Not Found", e.getMessage()))
                    .build();
        }

        if (exception instanceof TenantMismatchException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new ErrorResponse(403, "Forbidden", e.getMessage()))
                    .build();
        }

        if (exception instanceof jakarta.validation.ConstraintViolationException e) {
            String details = e.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Validation failed");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(400, "Validation Error", details))
                    .build();
        }

        // JAX-RS exceptions (404, 405, etc.) — pass through without logging as ERROR
        if (exception instanceof jakarta.ws.rs.WebApplicationException wae) {
            int status = wae.getResponse().getStatus();
            String reason = wae.getMessage() != null ? wae.getMessage() : "Request error";
            LOG.debugf("WebApplicationException [%d]: %s", status, reason);
            return Response.status(status)
                    .entity(new ErrorResponse(status, Response.Status.fromStatusCode(status).getReasonPhrase(), reason))
                    .build();
        }

        LOG.error("Unhandled exception", exception);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse(500, "Internal Server Error", "An unexpected error occurred"))
                .build();
    }
}
