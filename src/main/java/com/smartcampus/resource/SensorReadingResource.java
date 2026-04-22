/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.store.DataStore;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

/**
 * Sub-resource for managing historical readings of a specific sensor.
 *
 * This class is instantiated by the sub-resource locator in SensorResource.
 * It is NOT annotated with @Path at the class level — its path context
 * (/sensors/{sensorId}/readings) is inherited from the parent locator.
 *
 * The sensorId is passed via constructor injection from the parent resource,
 * establishing the reading context for all operations in this class.
 */
@Produces(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final DataStore store = DataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    /**
     * GET /sensors/{sensorId}/readings — Fetch all historical readings
     * for this sensor, ordered by insertion time.
     */
    @GET
    public List<SensorReading> getReadings() {
        return store.getReadings(sensorId);
    }

    /**
     * POST /sensors/{sensorId}/readings — Record a new reading.
     *
     * Business rules:
     * 1. The parent sensor must be in ACTIVE status. Sensors in MAINTENANCE
     *    or OFFLINE status cannot accept readings → 403 Forbidden.
     * 2. A UUID is generated for the reading ID if not provided.
     * 3. The timestamp defaults to the current epoch time if not provided.
     *
     * Side effect (Part 4.2):
     * After a successful POST, the parent sensor's currentValue field is
     * updated to match the new reading's value. This ensures data consistency
     * across the API — GET /sensors/{sensorId} always reflects the latest
     * measurement without requiring the client to make a separate update call.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addReading(SensorReading reading) {
        Sensor sensor = store.getSensor(sensorId);

        // Part 5.1 — 403 Forbidden for MAINTENANCE/OFFLINE sensors
        if (!"ACTIVE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(sensorId, sensor.getStatus());
        }

        // Generate ID if not provided
        if (reading.getId() == null || reading.getId().isBlank()) {
            reading.setId(UUID.randomUUID().toString());
        }

        // Default timestamp to now if not provided
        if (reading.getTimestamp() <= 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        // Store the reading
        store.addReading(sensorId, reading);

        // Side effect: update parent sensor's currentValue
        sensor.setCurrentValue(reading.getValue());

        return Response.status(Response.Status.CREATED).entity(reading).build();
    }
}
