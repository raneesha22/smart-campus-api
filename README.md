# Smart Campus — Sensor & Room Management API

A RESTful API for managing campus rooms, sensors, and sensor readings. Built with JAX-RS (Jersey) and an embedded Grizzly HTTP server — meaning you just run a single JAR file and the server starts, no external setup needed. All data lives in memory using ConcurrentHashMap, so nothing persists between restarts.

**Built with:** Java 17, JAX-RS (Jersey 3.1.5), Grizzly HTTP Server, Maven

---

## How to Build and Run

You need Java 17 and Maven installed.

```bash
git clone https://github.com/YOUR_USERNAME/smart-campus-api.git
cd smart-campus-api
mvn clean package
java -jar target/smart-campus-api-1.0.0.jar
```

Once running, the API is available at `http://localhost:8080/api/v1`

The API starts with some sample data already loaded — 3 rooms (LIB-301, ENG-102, LEC-201) and 4 sensors (TEMP-001, CO2-001, OCC-001, LIGHT-001) so you can test everything immediately.

---

## Packages

The code is split into six packages under `com.smartcampus`:

- **model** — the three data classes: Room, Sensor, SensorReading
- **store** — DataStore, the single shared in-memory data store
- **resource** — the four JAX-RS resource classes that handle all the endpoints
- **exception** — three custom exceptions covering the main error scenarios
- **mapper** — four exception mappers that turn exceptions into proper JSON responses
- **filter** — a logging filter that records every incoming request and outgoing response

---

## Endpoints

| Method | Path | What it does |
|--------|------|-------------|
| GET | `/api/v1` | API overview and links to all resources |
| GET | `/api/v1/rooms` | List all rooms |
| POST | `/api/v1/rooms` | Create a room |
| GET | `/api/v1/rooms/{roomId}` | Get a specific room |
| PUT | `/api/v1/rooms/{roomId}` | Update a room |
| DELETE | `/api/v1/rooms/{roomId}` | Delete a room |
| GET | `/api/v1/sensors` | List all sensors (supports `?type=` filter) |
| POST | `/api/v1/sensors` | Register a sensor |
| GET | `/api/v1/sensors/{sensorId}` | Get a specific sensor |
| PUT | `/api/v1/sensors/{sensorId}` | Update a sensor |
| DELETE | `/api/v1/sensors/{sensorId}` | Remove a sensor |
| GET | `/api/v1/sensors/{sensorId}/readings` | Get reading history for a sensor |
| POST | `/api/v1/sensors/{sensorId}/readings` | Record a new reading |
| GET | `/api/v1/sensors/{sensorId}/readings/{readingId}` | Get a specific reading |

---

## curl Examples

**1. Discovery**
```bash
curl http://localhost:8080/api/v1
```

**2. Create a room**
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"SCI-405","name":"Science Lab B","capacity":25}'
```

**3. Register a sensor**
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-050","type":"Temperature","status":"ACTIVE","currentValue":21.0,"roomId":"SCI-405"}'
```

**4. Filter sensors by type**
```bash
curl "http://localhost:8080/api/v1/sensors?type=Temperature"
```

**5. Post a reading**
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":28.3}'
```

**6. Try deleting a room that still has sensors — 409 Conflict**
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

**7. Try registering a sensor with a non-existent roomId — 422**
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-999","type":"Temperature","status":"ACTIVE","currentValue":20.0,"roomId":"FAKE-ROOM"}'
```

**8. Set a sensor to MAINTENANCE then try posting a reading — 403**
```bash
curl -X PUT http://localhost:8080/api/v1/sensors/LIGHT-001 \
  -H "Content-Type: application/json" \
  -d '{"status":"MAINTENANCE"}'

curl -X POST http://localhost:8080/api/v1/sensors/LIGHT-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":500.0}'
```

**9. Update a room**
```bash
curl -X PUT http://localhost:8080/api/v1/rooms/LIB-301 \
  -H "Content-Type: application/json" \
  -d '{"name":"Main Library South","capacity":55}'
```

**10. Trigger a 500 — proves the global safety net is working**
```bash
curl http://localhost:8080/api/v1/test-error
```

---

## Error Responses

Every error returns the same JSON structure so clients always know what to expect:

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Cannot delete room 'LIB-301': it still has 2 active sensor(s) assigned.",
  "timestamp": "2026-04-24T10:30:00Z"
}
```

| Status | When it happens |
|--------|----------------|
| 403 | Posting a reading to a sensor in MAINTENANCE or OFFLINE |
| 404 | Requesting a room or sensor ID that does not exist |
| 409 | Trying to delete a room that still has sensors |
| 415 | Sending the wrong Content-Type (not application/json) |
| 422 | Registering a sensor with a roomId that does not exist |
| 500 | Anything unexpected — caught by the global mapper, no stack trace exposed |

---

## Conceptual Questions

### Part 1 — Setup and Discovery

**What is the default lifecycle of a JAX-RS resource class, and how does that affect in-memory data management?**

JAX-RS creates a brand new instance of your resource class for every single request. That means any data you store as a field on the class disappears the moment the request is done — you cannot keep rooms and sensors stored inside the resource class itself.

To handle this, there is a separate DataStore class that acts as a singleton — one instance that lives for the entire application's lifetime. Every resource class calls `DataStore.getInstance()` to read and write data. The data sits in ConcurrentHashMap collections, which handle thread safety internally. This matters because the server processes requests on a thread pool, meaning two requests can arrive at the same moment. Without ConcurrentHashMap, two simultaneous POST requests could corrupt data — one thread reading stale values while another is mid-write. ConcurrentHashMap handles that synchronisation so the resource classes do not have to.

**Why is HATEOAS considered a mark of good API design, and what does it actually do for developers using the API?**

HATEOAS means your API responses include links to related resources, so clients can navigate without hardcoding every URI. The discovery endpoint at `GET /api/v1` is the clearest example — one call tells you where everything lives.

The practical benefit is resilience. If you change a URI structure, clients that follow links from the discovery response do not break. Developers also do not need to memorise endpoint paths — the API points them in the right direction. It is the same reason websites work the way they do: you start at a homepage and follow links rather than typing every URL from memory.

---

### Part 2 — Room Management

**When returning a list of rooms, what are the trade-offs between returning just IDs versus full objects?**

Returning only IDs keeps each response small, but forces the client to make a separate GET request for every single room to get any useful information. With 200 rooms that is suddenly 201 requests instead of 1 — the classic N+1 problem.

Returning full objects uses more bandwidth per response, but the client gets everything in one shot. For the kinds of systems that actually use this API — dashboards, building management tools, monitoring services — the full room data is always needed anyway. The slight increase in payload size is a reasonable trade-off for the significant reduction in round trips.

**Is your DELETE implementation idempotent?**

Yes. The first time you DELETE a room that exists and has no sensors, it is removed and you get 204 No Content. Sending the exact same request again returns 404 because the room is gone. The important thing is that the server's state does not change after that first call — the room is absent regardless of how many times you try. Getting a 404 on repeat calls is not a violation of idempotency, it simply confirms the goal was already achieved.

---

### Part 3 — Sensors and Filtering

**What happens if a client sends data as text/plain or XML when the endpoint expects application/json?**

JAX-RS checks the Content-Type header before it even runs your method. If the header does not match what is declared in `@Consumes(MediaType.APPLICATION_JSON)`, the framework automatically returns HTTP 415 Unsupported Media Type and your code never executes. No manual checking is needed — the annotation enforces it at the framework level.

**Why use @QueryParam for filtering instead of embedding the type in the URL path?**

Query parameters are optional by nature. `GET /sensors` and `GET /sensors?type=CO2` work through the same method — the parameter just is not there if you leave it out. Path-based filtering like `GET /sensors/type/CO2` would need a completely separate endpoint for the unfiltered list.

They also compose cleanly. Adding a status filter becomes `?type=CO2&status=ACTIVE`. The path equivalent would be `/sensors/type/CO2/status/ACTIVE` — clunky and implying an ordering that is completely arbitrary. In REST, path segments identify resources and query parameters refine or filter them.

---

### Part 4 — Sub-Resources

**What are the benefits of the sub-resource locator pattern?**

Instead of defining every reading-related path directly inside SensorResource, there is a single locator method that hands off anything under `/sensors/{sensorId}/readings` to a dedicated SensorReadingResource class. That class receives the sensorId through its constructor and handles all reading logic from there.

The main benefit is that each class stays focused on one thing. SensorResource handles sensors, SensorReadingResource handles readings. If pagination or date filtering needs to be added to readings later, you change one file without touching sensor logic. A single class handling every nested path grows quickly and becomes difficult to follow. The pattern also makes each class easier to test independently.

---

### Part 5 — Error Handling

**Why is 422 a better choice than 404 when a sensor references a roomId that does not exist?**

404 means the URL you requested does not exist. When a client POSTs to `/api/v1/sensors`, that URL is perfectly valid — the endpoint is there. The problem is not the URL, it is what is inside the request body: a reference to a room that does not exist.

422 Unprocessable Entity is the accurate response here. It tells the client: your JSON was valid, the request was understood, but the content cannot be processed because it references something that is not in the system. A developer seeing 404 would check their URL. A developer seeing 422 knows to look at the payload.

**What are the security risks of exposing raw Java stack traces to API consumers?**

A stack trace gives away more than it should. It reveals exact library versions, which lets attackers look up known vulnerabilities in those specific versions. It exposes internal package names and class structures — essentially a partial map of the codebase. It can include server file paths that reveal the operating system and directory layout. Method names in the call chain can also hint at how business logic flows, helping attackers craft inputs that target specific code paths.

The GlobalExceptionMapper catches anything not handled by a more specific mapper, logs the full details on the server side where they are useful for debugging, and sends the client a clean generic message. No internal information leaves the server.

**Why use a JAX-RS filter for logging instead of adding Logger calls to every method?**

Consistency is the main reason. A filter runs for every request automatically — you cannot forget to add it. Manual Logger calls require a developer to remember to add them to every new method, and one missed endpoint creates a gap in the logs.

There is also a maintainability angle. If you want to change what gets logged — add timing, include request IDs, switch to a different logging library — you change one file. With manual logging scattered across every resource method, that same change means touching dozens of places. Keeping logging in one class keeps the resource methods focused on the business logic they are actually there to handle.