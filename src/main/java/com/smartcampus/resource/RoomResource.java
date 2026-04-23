/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.resource;

import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.store.DataStore;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the /rooms collection.
 * Handles creation, listing, detail retrieval, and safe deletion of rooms.
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore store = DataStore.getInstance();

    /**
     * GET /rooms — List all rooms.
     * Returns full room objects (not just IDs) so clients have all the
     * information they need in a single request. This trades slightly
     * more bandwidth for fewer round trips, which is the better default
     * for most REST APIs.
     */
    @GET
    public List<Room> getAllRooms() {
        return new ArrayList<>(store.getRooms().values());
    }

    /**
     * POST /rooms — Create a new room.
     * Returns 201 Created with a Location header pointing to the new resource.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createRoom(Room room, @Context UriInfo uriInfo) {
        if (room.getId() == null || room.getId().isBlank()) {
            throw new BadRequestException("Room ID is required.");
        }
        if (store.roomExists(room.getId())) {
            throw new BadRequestException("Room with ID '" + room.getId() + "' already exists.");
        }

        store.addRoom(room);

        URI location = uriInfo.getAbsolutePathBuilder().path(room.getId()).build();
        return Response.created(location).entity(room).build();
    }

    /**
     * GET /rooms/{roomId} — Fetch detailed metadata for a specific room.
     * Returns 404 if the room does not exist.
     */
    @GET
    @Path("/{roomId}")
    public Room getRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRoom(roomId);
        if (room == null) {
            throw new NotFoundException("Room '" + roomId + "' not found.");
        }
        return room;
    }

    /**
     * DELETE /rooms/{roomId} — Delete a room.
     *
     * Business Logic Constraint: A room cannot be deleted if it still has
     * sensors assigned. This prevents orphaned sensor records that would
     * reference a non-existent room.
     *
     * Idempotency: This implementation is idempotent. The first DELETE
     * removes the room and returns 204 No Content. Subsequent identical
     * DELETE requests for the same ID return 404 Not Found, which is
     * acceptable for idempotent operations — the end state (room does
     * not exist) is the same regardless of how many times the request
     * is sent. The server's state does not change after the first call.
     */
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRoom(roomId);
        if (room == null) {
            throw new NotFoundException("Room '" + roomId + "' not found.");
        }

        // Block deletion if room still has sensors (Part 5.1 — 409 Conflict)
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId, room.getSensorIds().size());
        }

        store.removeRoom(roomId);
        return Response.noContent().build();
    }
    
    @PUT
@Path("/{roomId}")
@Consumes(MediaType.APPLICATION_JSON)
public Room updateRoom(@PathParam("roomId") String roomId, Room updated) {
    Room existing = store.getRoom(roomId);
    if (existing == null) {
        throw new NotFoundException("Room '" + roomId + "' not found.");
    }
    if (updated.getName() != null) existing.setName(updated.getName());
    if (updated.getCapacity() > 0) existing.setCapacity(updated.getCapacity());
    return existing;
}
}
