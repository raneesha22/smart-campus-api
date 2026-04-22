/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Root discovery endpoint.
 * Returns API metadata and a map of primary resource collection URIs,
 * enabling HATEOAS-style navigation for client developers.
 */
@Path("/")
public class DiscoveryResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> discover() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("api", "Smart Campus Sensor & Room Management API");
        response.put("version", "1.0.0");
        response.put("description", "RESTful service for managing campus rooms, sensors, and sensor readings.");
        response.put("contact", "admin@smartcampus.ac.uk");

        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("rooms", "/api/v1/rooms");
        resources.put("sensors", "/api/v1/sensors");
        response.put("resources", resources);

        Map<String, String> docs = new LinkedHashMap<>();
        docs.put("rooms_collection", "GET /api/v1/rooms — List all rooms");
        docs.put("room_detail", "GET /api/v1/rooms/{roomId} — Get room details");
        docs.put("sensors_collection", "GET /api/v1/sensors — List all sensors (optional ?type= filter)");
        docs.put("sensor_readings", "GET /api/v1/sensors/{sensorId}/readings — Sensor reading history");
        response.put("endpoints", docs);

        return response;
    }
}
