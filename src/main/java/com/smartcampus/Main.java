/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point for the Smart Campus API.
 * Starts an embedded Grizzly HTTP server with the JAX-RS application.
 */
public class Main {

    private static final String BASE_URI = "http://localhost:8080/";
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static HttpServer startServer() {
        ResourceConfig config = ResourceConfig.forApplicationClass(SmartCampusApplication.class);
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), config);
    }

    public static void main(String[] args) {
        try {
            HttpServer server = startServer();
            LOGGER.info("Smart Campus API started at " + BASE_URI + "api/v1");
            LOGGER.info("Press Ctrl+C to stop the server...");

            // Keep the server alive until interrupted
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Server interrupted", e);
            Thread.currentThread().interrupt();
        }
    }
}
