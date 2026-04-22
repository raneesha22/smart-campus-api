/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.mapper;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global catch-all exception mapper.
 * Intercepts any unexpected RuntimeException or Error (NullPointerException,
 * IndexOutOfBoundsException, etc.) and returns a clean HTTP 500 response
 * with a generic JSON body. The raw stack trace is NEVER exposed to the
 * client — it is logged server-side only.
 *
 * Security rationale:
 * Exposing raw Java stack traces reveals internal implementation details
 * such as package names, class structures, library versions, and file
 * paths. An attacker could use this information to identify known
 * vulnerabilities in specific library versions, map the internal
 * architecture for further exploitation, or craft targeted injection
 * attacks based on the exposed class hierarchy.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable ex) {
        // Log full details server-side for debugging
        LOGGER.log(Level.SEVERE, "Unhandled exception caught by global mapper", ex);

        ErrorResponse error = new ErrorResponse(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Internal Server Error",
                "An unexpected error occurred. Please contact the system administrator."
        );
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
