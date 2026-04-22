/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.exception;

/**
 * Thrown when attempting to delete a Room that still has sensors assigned.
 * Maps to HTTP 409 Conflict.
 */
public class RoomNotEmptyException extends RuntimeException {
    public RoomNotEmptyException(String roomId, int sensorCount) {
        super("Cannot delete room '" + roomId + "': it still has " + sensorCount
                + " active sensor(s) assigned. Remove or reassign all sensors before deleting the room.");
    }
}

