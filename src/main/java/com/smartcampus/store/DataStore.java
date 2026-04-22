/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.store;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralised in-memory data store for the Smart Campus API.
 *
 * Uses ConcurrentHashMap for thread-safe access across concurrent requests.
 * JAX-RS creates a new resource instance per request (per-request lifecycle),
 * so this singleton store is the shared state that all resource instances read
 * from and write to. ConcurrentHashMap handles synchronisation internally,
 * preventing race conditions without requiring explicit locking.
 *
 * Sensor readings are stored in a separate map keyed by sensor ID, with each
 * value being a list of SensorReading objects representing the historical log.
 */
public final class DataStore {

    private static final DataStore INSTANCE = new DataStore();

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    private final Map<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    private DataStore() {
        // Seed some sample data so the API has content on startup
        seedData();
    }

    public static DataStore getInstance() {
        return INSTANCE;
    }

    // ── Room operations ──────────────────────────────────────────────

    public Map<String, Room> getRooms() {
        return rooms;
    }

    public Room getRoom(String id) {
        return rooms.get(id);
    }

    public void addRoom(Room room) {
        rooms.put(room.getId(), room);
    }

    public Room removeRoom(String id) {
        return rooms.remove(id);
    }

    public boolean roomExists(String id) {
        return rooms.containsKey(id);
    }

    // ── Sensor operations ────────────────────────────────────────────

    public Map<String, Sensor> getSensors() {
        return sensors;
    }

    public Sensor getSensor(String id) {
        return sensors.get(id);
    }

    public void addSensor(Sensor sensor) {
        sensors.put(sensor.getId(), sensor);
        readings.putIfAbsent(sensor.getId(), new ArrayList<>());
    }

    public Sensor removeSensor(String id) {
        readings.remove(id);
        return sensors.remove(id);
    }

    public boolean sensorExists(String id) {
        return sensors.containsKey(id);
    }

    // ── Reading operations ───────────────────────────────────────────

    public List<SensorReading> getReadings(String sensorId) {
        return readings.getOrDefault(sensorId, new ArrayList<>());
    }

    public void addReading(String sensorId, SensorReading reading) {
        readings.computeIfAbsent(sensorId, k -> new ArrayList<>()).add(reading);
    }

    // ── Seed data ────────────────────────────────────────────────────

    private void seedData() {
        Room lib301 = new Room("LIB-301", "Library Quiet Study", 40);
        Room eng102 = new Room("ENG-102", "Engineering Lab A", 30);
        Room lec201 = new Room("LEC-201", "Main Lecture Hall", 200);
        rooms.put(lib301.getId(), lib301);
        rooms.put(eng102.getId(), eng102);
        rooms.put(lec201.getId(), lec201);

        Sensor temp001 = new Sensor("TEMP-001", "Temperature", "ACTIVE", 22.5, "LIB-301");
        Sensor co2001 = new Sensor("CO2-001", "CO2", "ACTIVE", 415.0, "LIB-301");
        Sensor occ001 = new Sensor("OCC-001", "Occupancy", "ACTIVE", 18.0, "ENG-102");
        Sensor light001 = new Sensor("LIGHT-001", "Lighting", "MAINTENANCE", 750.0, "LEC-201");

        addSensor(temp001);
        addSensor(co2001);
        addSensor(occ001);
        addSensor(light001);

        lib301.addSensorId("TEMP-001");
        lib301.addSensorId("CO2-001");
        eng102.addSensorId("OCC-001");
        lec201.addSensorId("LIGHT-001");
    }
}
