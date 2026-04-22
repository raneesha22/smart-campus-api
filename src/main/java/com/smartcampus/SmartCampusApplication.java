/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import java.util.HashSet;
import java.util.Set;

import com.smartcampus.resource.DiscoveryResource;
import com.smartcampus.resource.RoomResource;
import com.smartcampus.resource.SensorResource;
import com.smartcampus.mapper.RoomNotEmptyExceptionMapper;
import com.smartcampus.mapper.LinkedResourceNotFoundExceptionMapper;
import com.smartcampus.mapper.SensorUnavailableExceptionMapper;
import com.smartcampus.mapper.GlobalExceptionMapper;
import com.smartcampus.filter.LoggingFilter;

/**
 * JAX-RS Application configuration.
 *
 * The @ApplicationPath annotation establishes the versioned API root.
 * All resource URIs are relative to /api/v1.
 *
 * JAX-RS Resource Lifecycle:
 * By default, JAX-RS creates a NEW instance of each resource class for every
 * incoming request (per-request lifecycle). This means instance fields are NOT
 * shared between concurrent requests. To safely share state (our in-memory data
 * store), we use a singleton DataStore class with ConcurrentHashMap, which is
 * accessed via a static method. This avoids race conditions without requiring
 * synchronisation on the resource classes themselves.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();

        // Resources
        classes.add(DiscoveryResource.class);
        classes.add(RoomResource.class);
        classes.add(SensorResource.class);

        // Exception Mappers (Part 5 — 30 marks)
        classes.add(RoomNotEmptyExceptionMapper.class);
        classes.add(LinkedResourceNotFoundExceptionMapper.class);
        classes.add(SensorUnavailableExceptionMapper.class);
        classes.add(GlobalExceptionMapper.class);

        // Logging Filter
        classes.add(LoggingFilter.class);

        return classes;
    }
}
