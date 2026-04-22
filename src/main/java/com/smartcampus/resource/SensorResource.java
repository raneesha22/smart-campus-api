/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.store.DataStore;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages the /sensors collection.
 * Handles registration with foreign key validation, filtered retrieval,
 * and delegates to SensorReadingResource via the sub-resource locator pattern.
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.getInstance();

    /**
     * GET /sensors — List all sensors.
     * Supports optional query parameter ?type= for filtered retrieval.
     *
     * The @QueryParam approach is preferred over path-based filtering
     * (e.g. /sensors/type/CO2) because:
     * 1. Query parameters are optional by nature — omitting ?type= returns
     *    all sensors. A path segment would require a separate endpoint for
     *    the unfiltered list.
     * 2. Multiple filters compose naturally: ?type=CO2&status=ACTIVE.
     *    Path-based filtering would create a combinatorial explosion of routes.
     * 3. It follows REST convention: path segments identify resources,
     *    query parameters refine/filter collections.
     */
    @GET
    public List<Sensor> getAllSensors(@QueryParam("type") String type) {
        List<Sensor> all = new ArrayList<>(store.getSensors().values());

        if (type != null && !type.isBlank()) {
            return all.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }

        return all;
    }

    /**
     * POST /sensors — Register a new sensor.
     *
     * The @Consumes annotation enforces that only application/json payloads
     * are accepted. If a client sends text/plain or application/xml, JAX-RS
     * automatically returns HTTP 415 Unsupported Media Type before the method
     * body executes. The runtime checks the Content-Type header against the
     * declared @Consumes value and rejects mismatches at the framework level.
     *
     * Business rule: the roomId in the payload must reference an existing room.
     * If not, a LinkedResourceNotFoundException is thrown → 422 Unprocessable Entity.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor, @Context UriInfo uriInfo) {
        if (sensor.getId() == null || sensor.getId().isBlank()) {
            throw new BadRequestException("Sensor ID is required.");
        }
        if (store.sensorExists(sensor.getId())) {
            throw new BadRequestException("Sensor with ID '" + sensor.getId() + "' already exists.");
        }

        // Validate that the referenced room exists (Part 5.2 — 422)
        if (sensor.getRoomId() == null || !store.roomExists(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException("room", sensor.getRoomId());
        }

        // Set default status if not provided
        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            sensor.setStatus("ACTIVE");
        }

        store.addSensor(sensor);

        // Link sensor to the parent room
        Room room = store.getRoom(sensor.getRoomId());
        room.addSensorId(sensor.getId());

        URI location = uriInfo.getAbsolutePathBuilder().path(sensor.getId()).build();
        return Response.created(location).entity(sensor).build();
    }

    /**
     * GET /sensors/{sensorId} — Fetch a single sensor's details.
     */
    @GET
    @Path("/{sensorId}")
    public Sensor getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor '" + sensorId + "' not found.");
        }
        return sensor;
    }

    /**
     * DELETE /sensors/{sensorId} — Remove a sensor.
     * Also removes the sensor ID from its parent room's sensorIds list.
     */
    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor '" + sensorId + "' not found.");
        }

        // Unlink from parent room
        Room room = store.getRoom(sensor.getRoomId());
        if (room != null) {
            room.removeSensorId(sensorId);
        }

        store.removeSensor(sensorId);
        return Response.noContent().build();
    }

    /**
     * Sub-resource locator for sensor readings.
     *
     * The sub-resource locator pattern delegates all requests under
     * /sensors/{sensorId}/readings to a dedicated SensorReadingResource class.
     *
     * Architectural benefits:
     * 1. Separation of concerns — reading logic is isolated from sensor logic.
     *    Each class has a single responsibility.
     * 2. Scalability — as the readings API grows (pagination, aggregation),
     *    changes are confined to SensorReadingResource without bloating this class.
     * 3. Testability — each resource class can be unit tested independently.
     * 4. Readability — a large monolithic controller with paths like
     *    /sensors/{id}/readings, /sensors/{id}/readings/{rid},
     *    /sensors/{id}/readings/latest, etc. becomes unmanageable.
     *    Delegation keeps each class focused and small.
     *
     * Note: This method has NO HTTP method annotation (@GET, @POST, etc.)
     * That is the hallmark of a sub-resource locator — JAX-RS delegates
     * HTTP method resolution to the returned resource class.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsSubResource(@PathParam("sensorId") String sensorId) {
        // Validate sensor exists before delegating
        if (!store.sensorExists(sensorId)) {
            throw new NotFoundException("Sensor '" + sensorId + "' not found.");
        }
        return new SensorReadingResource(sensorId);
    }
}
