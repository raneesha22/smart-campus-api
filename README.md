#Smart Campus — Sensor & Room Management API

A high-performance RESTful API built with **JAX-RS (Jersey 3.1.5)** and an embedded **Grizzly HTTP server** for managing campus rooms, sensors, and sensor readings. Designed for the university's Smart Campus initiative to provide facilities managers and automated building systems with a seamless interface to campus infrastructure data.

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Framework | JAX-RS (Jersey 3.1.5) |
| Server | Grizzly Embedded HTTP Server |
| Build | Maven with Shade Plugin |
| Language | Java 17 |
| Data Storage | ConcurrentHashMap (in-memory) |
| JSON | Jackson (via jersey-media-json-jackson) |

## Project Structure

```
smart-campus-api/
├── pom.xml
└── src/main/java/com/smartcampus/
    ├── Main.java                          # Entry point — boots Grizzly server
    ├── SmartCampusApplication.java        # JAX-RS Application config
    ├── model/
    │   ├── Room.java                      # Room entity
    │   ├── Sensor.java                    # Sensor entity
    │   └── SensorReading.java            # Reading event entity
    ├── store/
    │   └── DataStore.java                 # Thread-safe singleton data store
    ├── resource/
    │   ├── DiscoveryResource.java         # GET /api/v1 — API metadata
    │   ├── RoomResource.java              # /api/v1/rooms — CRUD
    │   ├── SensorResource.java            # /api/v1/sensors — CRUD + filtering
    │   └── SensorReadingResource.java     # Sub-resource for sensor readings
    ├── exception/
    │   ├── RoomNotEmptyException.java     # 409 Conflict
    │   ├── LinkedResourceNotFoundException.java  # 422 Unprocessable Entity
    │   └── SensorUnavailableException.java       # 403 Forbidden
    ├── mapper/
    │   ├── ErrorResponse.java             # Standardised JSON error body
    │   ├── RoomNotEmptyExceptionMapper.java
    │   ├── LinkedResourceNotFoundExceptionMapper.java
    │   ├── SensorUnavailableExceptionMapper.java
    │   └── GlobalExceptionMapper.java     # Catch-all 500 safety net
    └── filter/
        └── LoggingFilter.java             # Request/response logging
```

## How to Build and Run

### Prerequisites
- Java 17 or later
- Apache Maven 3.8+

### Build

```bash
git clone https://github.com/YOUR_USERNAME/smart-campus-api.git
cd smart-campus-api
mvn clean package
```

### Run

```bash
java -jar target/smart-campus-api-1.0.0.jar
```

The server starts at `http://localhost:8080/api/v1`. You should see:

```
Smart Campus API started at http://localhost:8080/api/v1
Press Ctrl+C to stop the server...
```

The API comes pre-loaded with sample data: 3 rooms (LIB-301, ENG-102, LEC-201) and 4 sensors (TEMP-001, CO2-001, OCC-001, LIGHT-001).

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1` | Discovery — API metadata and resource links |
| GET | `/api/v1/rooms` | List all rooms |
| POST | `/api/v1/rooms` | Create a new room |
| GET | `/api/v1/rooms/{roomId}` | Get room details |
| DELETE | `/api/v1/rooms/{roomId}` | Delete a room (if no sensors assigned) |
| GET | `/api/v1/sensors` | List all sensors (optional `?type=` filter) |
| POST | `/api/v1/sensors` | Register a new sensor |
| GET | `/api/v1/sensors/{sensorId}` | Get sensor details |
| DELETE | `/api/v1/sensors/{sensorId}` | Remove a sensor |
| GET | `/api/v1/sensors/{sensorId}/readings` | Get reading history |
| POST | `/api/v1/sensors/{sensorId}/readings` | Record a new reading |

---

## Sample curl Commands

### 1. Discovery endpoint

```bash
curl -s http://localhost:8080/api/v1 | python3 -m json.tool
```

### 2. Create a new room

```bash
curl -s -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"SCI-405","name":"Science Lab B","capacity":25}' | python3 -m json.tool
```

### 3. Register a sensor in the new room

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-050","type":"Temperature","status":"ACTIVE","currentValue":21.0,"roomId":"SCI-405"}' | python3 -m json.tool
```

### 4. Get sensors filtered by type

```bash
curl -s "http://localhost:8080/api/v1/sensors?type=Temperature" | python3 -m json.tool
```

### 5. Post a reading and verify the side effect

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":28.3}' | python3 -m json.tool
```

Then verify the parent sensor's currentValue was updated:

```bash
curl -s http://localhost:8080/api/v1/sensors/TEMP-001 | python3 -m json.tool
```

### 6. Error scenario — attempt to delete a room with sensors (409 Conflict)

```bash
curl -s -X DELETE http://localhost:8080/api/v1/rooms/LIB-301 | python3 -m json.tool
```

Expected response:
```json
{
    "status": 409,
    "error": "Conflict",
    "message": "Cannot delete room 'LIB-301': it still has 2 active sensor(s) assigned. Remove or reassign all sensors before deleting the room."
}
```

### 7. Error scenario — register sensor with non-existent room (422)

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-999","type":"Temperature","status":"ACTIVE","currentValue":20.0,"roomId":"FAKE-ROOM"}' | python3 -m json.tool
```

### 8. Error scenario — post reading to a MAINTENANCE sensor (403)

```bash
curl -s -X POST http://localhost:8080/api/v1/sensors/LIGHT-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":500.0}' | python3 -m json.tool
```

---

## Conceptual Questions & Answers

### Part 1: Service Architecture & Setup

**Q: Explain the default lifecycle of a JAX-RS Resource class. Is a new instance created for every request, or is it a singleton? How does this impact in-memory data management?**

By default, JAX-RS uses a **per-request lifecycle** — a new instance of each resource class is created for every incoming HTTP request. This means instance variables on a resource class are not shared between concurrent requests and are garbage collected after the response is sent.

This design decision has a direct impact on how we manage shared state. Since each resource instance is independent, we cannot store application data (rooms, sensors, readings) as instance fields on the resource classes — the data would be lost after each request. Instead, we use a **singleton DataStore** class that holds all data in `ConcurrentHashMap` collections. Every resource instance accesses this shared store via `DataStore.getInstance()`.

`ConcurrentHashMap` was chosen specifically because it provides thread-safe read and write operations without requiring explicit synchronisation blocks. Multiple request threads can safely read and modify the data concurrently. This is critical because the embedded Grizzly server processes requests on a thread pool — without thread-safe data structures, concurrent POST and DELETE operations could corrupt the data or produce race conditions.

**Q: Why is HATEOAS considered a hallmark of advanced RESTful design? How does it benefit client developers?**

HATEOAS (Hypermedia as the Engine of Application State) means that API responses include links to related resources and available actions, allowing clients to navigate the API dynamically rather than hardcoding URIs.

Our discovery endpoint at `GET /api/v1` demonstrates this principle by returning a map of resource collection URIs. A client developer can call this single endpoint to discover all available resources without reading external documentation. This provides several benefits: clients are resilient to URI changes (if we restructure paths, only the discovery response changes, not the client code), new resource collections are immediately discoverable without documentation updates, and client developers can build generic API browsers that follow links rather than constructing URIs manually. This is the same principle that makes the web navigable — HTML pages contain links to other pages, and browsers follow them. HATEOAS applies this concept to APIs.

### Part 2: Room Management

**Q: When returning a list of rooms, what are the implications of returning only IDs versus full objects?**

Returning **only IDs** minimises bandwidth — a list of 1000 room IDs might be a few kilobytes, while 1000 full room objects with all fields could be tens of kilobytes. However, it forces the client to make N additional GET requests to fetch each room's details, creating an N+1 query problem that dramatically increases total latency and server load.

Returning **full objects** (our approach) uses more bandwidth per request but enables the client to render the complete room list in a single round trip. For most use cases — dashboards, management UIs, monitoring systems — the client needs the full data anyway. The marginal bandwidth cost of including names, capacities, and sensor ID lists is negligible compared to the latency cost of hundreds of follow-up requests. This is the standard REST convention: collection endpoints return full representations unless the resource objects are extremely large, in which case pagination and field selection (sparse fieldsets) provide better solutions than ID-only responses.

**Q: Is the DELETE operation idempotent in your implementation?**

Yes. HTTP DELETE is defined as idempotent, meaning that making the same request multiple times produces the same end state as making it once. In our implementation:

- **First DELETE request** for a valid, empty room: the room is removed from the data store, and the server returns `204 No Content`.
- **Second (identical) DELETE request** for the same room ID: the room no longer exists, so the server returns `404 Not Found`.
- **Any subsequent DELETE requests**: the same `404 Not Found`.

The key point is that the server's state does not change after the first successful deletion. The 404 response on subsequent attempts is not a violation of idempotency — it simply indicates that the desired end state (room does not exist) has already been achieved. The server-side resource state is identical regardless of whether the DELETE was called once or ten times.

### Part 3: Sensors & Filtering

**Q: What are the technical consequences if a client sends data in a format other than application/json when @Consumes(APPLICATION_JSON) is specified?**

When a resource method is annotated with `@Consumes(MediaType.APPLICATION_JSON)`, JAX-RS enforces content type matching at the framework level, before the method body executes. If a client sends a request with `Content-Type: text/plain` or `Content-Type: application/xml`, the JAX-RS runtime cannot find a matching resource method and returns **HTTP 415 Unsupported Media Type** automatically.

This is handled by the JAX-RS content negotiation mechanism: the runtime inspects the `Content-Type` header of the incoming request, compares it against all `@Consumes` annotations on candidate methods, and only dispatches to a method where the media types match. If no match is found, the 415 response is generated without any custom code. This is beneficial because it enforces input format constraints declaratively — the developer does not need to write manual Content-Type checking logic, and the API contract is self-documenting through the annotations.

**Q: Why is @QueryParam generally considered superior to path-based filtering for collection searches?**

Query parameters are inherently **optional** — `GET /sensors` returns all sensors, and `GET /sensors?type=CO2` returns a filtered subset, using the same endpoint. With path-based filtering (`GET /sensors/type/CO2`), a separate unfiltered endpoint would be required, and the path structure implies a resource hierarchy that does not actually exist (there is no "type" sub-resource).

Query parameters also **compose naturally**. Adding a status filter produces `GET /sensors?type=CO2&status=ACTIVE` — a single, readable URI. Path-based equivalents (`GET /sensors/type/CO2/status/ACTIVE`) create a combinatorial explosion of route definitions and imply an ordering dependency (type before status) that is arbitrary. RESTful convention reserves path segments for resource identification and hierarchy (e.g., `/sensors/{id}/readings`), while query parameters handle filtering, sorting, and pagination — concerns that modify a collection view without identifying a specific resource.

### Part 4: Sub-Resources

**Q: Discuss the architectural benefits of the Sub-Resource Locator pattern.**

The sub-resource locator pattern delegates all requests under a nested path to a separate, dedicated class. In our implementation, `SensorResource` handles `/sensors/{sensorId}` and delegates `/sensors/{sensorId}/readings` to `SensorReadingResource` via a locator method that returns a new instance of the sub-resource class, passing the sensor ID through the constructor.

The primary benefits are:

**Separation of concerns:** Sensor CRUD logic and reading history logic are in different classes. Each class has a single responsibility and can be modified independently.

**Scalability of codebase:** As the readings API grows (pagination, aggregation, date-range filtering), all changes are confined to `SensorReadingResource`. Without delegation, a single monolithic `SensorResource` class would accumulate dozens of methods for paths like `/sensors/{id}/readings`, `/sensors/{id}/readings/{rid}`, `/sensors/{id}/readings/latest`, making the class unmanageable.

**Testability:** Each resource class can be unit tested independently with mock data stores.

**Reusability:** The sub-resource class could potentially be reused with different parent resources if the API evolves to support readings from other entity types.

### Part 5: Error Handling

**Q: Why is HTTP 422 often more semantically accurate than 404 when the issue is a missing reference inside a valid JSON payload?**

HTTP 404 means the **request URI itself** could not be matched to a resource — the endpoint does not exist. When a client POSTs a new sensor to `/api/v1/sensors` with a `roomId` that does not exist, the URI `/api/v1/sensors` is perfectly valid and the endpoint exists. The problem is not with the URI but with the **content** of the request body.

HTTP 422 (Unprocessable Entity) communicates precisely this: the server understood the request format (valid JSON, correct Content-Type), successfully parsed the payload, but cannot process it because the content is **semantically invalid** — it references a room that does not exist in the system. This distinction is important for client developers: a 404 might lead them to check their URL construction, while a 422 correctly directs them to inspect their request payload.

**Q: From a cybersecurity standpoint, what are the risks of exposing internal Java stack traces?**

Exposing raw stack traces to API consumers creates several security vulnerabilities:

**Library version disclosure:** Stack traces reveal exact library versions (e.g., `jersey-server-3.1.5.jar`), allowing attackers to search for known CVEs in those specific versions.

**Internal architecture mapping:** Package names and class structures (e.g., `com.smartcampus.store.DataStore`) expose the internal organisation, making it easier to identify attack surfaces like data access layers or authentication modules.

**File path disclosure:** Traces may include absolute file paths, revealing the server's operating system, directory structure, and deployment configuration.

**Logic inference:** Method names and call chains can reveal business logic flow, helping attackers craft targeted payloads that exploit specific code paths.

Our `GlobalExceptionMapper` prevents all of this by intercepting every unhandled `Throwable`, logging the full details server-side for debugging, and returning only a generic JSON message to the client.

**Q: Why is it advantageous to use JAX-RS filters for logging rather than manual Logger.info() statements?**

JAX-RS filters implement cross-cutting concerns at the framework level, providing three key advantages over manual logging:

**Consistency:** The filter intercepts every request and response automatically, including new endpoints added in the future. Manual logging requires developers to remember to add `Logger.info()` calls to every new method — a single omission creates a blind spot.

**Single Responsibility:** Logging logic is centralised in one class (`LoggingFilter`). Changing the log format, adding timing information, or switching to a different logging framework requires editing one file instead of every resource method.

**Separation of Concerns:** Resource methods focus purely on business logic. They do not contain infrastructure code for logging, timing, or request tracking. This makes the business logic easier to read, test, and maintain.

---

## Error Response Format

All errors return a consistent JSON structure:

```json
{
    "status": 409,
    "error": "Conflict",
    "message": "Cannot delete room 'LIB-301': it still has 2 active sensor(s) assigned."
}
```

| HTTP Status | Exception | Trigger |
|-------------|-----------|---------|
| 403 Forbidden | SensorUnavailableException | POST reading to MAINTENANCE sensor |
| 404 Not Found | NotFoundException (built-in) | Resource ID does not exist |
| 409 Conflict | RoomNotEmptyException | DELETE room with assigned sensors |
| 415 Unsupported Media Type | (JAX-RS automatic) | Wrong Content-Type header |
| 422 Unprocessable Entity | LinkedResourceNotFoundException | Sensor references non-existent roomId |
| 500 Internal Server Error | GlobalExceptionMapper | Any unhandled exception |

