/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.exception;

/**
 * Thrown when a client attempts to post a reading to a sensor
 * that is currently in MAINTENANCE or OFFLINE status.
 * Maps to HTTP 403 Forbidden.
 */
public class SensorUnavailableException extends RuntimeException {
    public SensorUnavailableException(String sensorId, String status) {
        super("Sensor '" + sensorId + "' is currently in '" + status
                + "' state and cannot accept new readings. Only ACTIVE sensors accept readings.");
    }
}
