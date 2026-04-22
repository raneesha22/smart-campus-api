/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Combined request and response logging filter.
 *
 * Implements both ContainerRequestFilter and ContainerResponseFilter to
 * provide full API observability as a cross-cutting concern. Using a JAX-RS
 * filter is superior to manually inserting Logger.info() calls in every
 * resource method because:
 *
 * 1. Single Responsibility — logging logic lives in one class, not scattered
 *    across every endpoint.
 * 2. Consistency — every request is logged, even new endpoints added later.
 *    Manual logging requires developers to remember to add it.
 * 3. Maintainability — changing the log format or adding fields (e.g. timing)
 *    requires editing one file instead of every resource class.
 * 4. Separation of Concerns — resource methods focus on business logic,
 *    not infrastructure concerns.
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOGGER.info("REQUEST  → " + requestContext.getMethod() + " " + requestContext.getUriInfo().getRequestUri());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        LOGGER.info("RESPONSE ← " + requestContext.getMethod() + " " + requestContext.getUriInfo().getRequestUri()
                + " → " + responseContext.getStatus());
    }
}
