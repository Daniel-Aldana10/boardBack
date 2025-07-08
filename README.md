# Collaborative Drawing Board - Backend

This is a Java Spring Boot backend for a real-time collaborative drawing board using WebSockets. It allows multiple users to draw on a shared canvas, view each other's updates in real time, and clear the canvas when needed.

## Features

-  WebSocket endpoint for real-time communication
-  Broadcasts draw events to all connected clients
-  Stores draw history in memory (using a `List<String>`)
-  Re-sends drawing history to new clients upon connection
- Supports canvas clearing, which also resets the stored history

## Requirements

Before you begin, ensure you have the following installed:

- **Java 17** or higher
- **Maven 3.6** or higher
- **Git**

Check with:

```bash
java -version
mvn -version
```

---

##  Project Structure
```
src/
└── main/
└── java/
└── edu/demo/board/
├── BBEndpoint.java # WebSocket server endpoint
├── BBConfigurator.java # WebSocket configuration
```

## WebSocket Endpoint

- **URL:** `ws://<server-address>/bbService`
- **Protocol:** Standard WebSocket
- **Message format:** JSON

### Supported Message Types

#### 1. Draw

```json
{
  "type": "draw",
  "x": 120,
  "y": 250,
  "color": "#000000",
  "size": 20
}
```
- Broadcasts the drawing to all other clients

- Stores the event in the in-memory history

2. Clear
```json
{
  "type": "clear"
}
```
Clears the canvas for all clients

Erases the drawing history from memory

3. Info (sent by server on connection)
```json
{
  "type": "info",
  "message": "Connection established."
}
```

## Installation

1. Clone the repository:

```bash
git clone https://github.com/Daniel-Aldana10/boardBack
cd boardBack
```

2. Build the project:

```bash
mvn clean package
```

3. Run the application:

```bash
mvn spring-boot:run
```

By default, it will be available at:  
`http://localhost:8080/`

---

## Author

Daniel Aldana — [GitHub](https://github.com/Daniel-Aldana10)

---

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
