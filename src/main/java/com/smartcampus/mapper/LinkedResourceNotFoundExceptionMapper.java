/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.mapper;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps LinkedResourceNotFoundException to HTTP 422 Unprocessable Entity.
 * Triggered when a sensor references a roomId that does not exist.
 *
 * Why 422 instead of 404:
 * HTTP 404 means the request URI itself could not be found. Here, the URI
 * (/api/v1/sensors) is valid — the problem is a missing reference INSIDE
 * the request payload. HTTP 422 communicates that the server understood
 * the request format (valid JSON) but cannot process it because the
 * content is semantically invalid (the referenced room does not exist).
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
                422,
                "Unprocessable Entity",
                ex.getMessage()
        );
        return Response.status(422)
                .entity(error)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}

