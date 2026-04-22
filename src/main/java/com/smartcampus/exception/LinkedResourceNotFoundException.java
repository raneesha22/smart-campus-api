/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.exception;

/**
 * Thrown when a client provides a reference (e.g. roomId) to a resource
 * that does not exist in the system.
 * Maps to HTTP 422 Unprocessable Entity.
 */
public class LinkedResourceNotFoundException extends RuntimeException {
    public LinkedResourceNotFoundException(String resourceType, String resourceId) {
        super("The referenced " + resourceType + " '" + resourceId
                + "' does not exist. Please provide a valid " + resourceType + " ID.");
    }
}
