# Collaborative Drawing Board - Backend

This is a Java Spring Boot backend for a real-time collaborative drawing board using WebSockets. It allows multiple users to draw on a shared canvas, view each other's updates in real time, and clear the canvas when needed.

## Features

- WebSocket endpoint for real-time communication
- Broadcasts draw events to all connected clients
- Stores draw history in memory (using a `List<String>`)
- Re-sends drawing history to new clients upon connection
- Supports canvas clearing, which also resets the stored history
- **Google OAuth2 authentication for secure WebSocket access**
- **Ticket-based protocol for WebSocket authentication**


## Requirements

Before you begin, ensure you have the following installed:

- **Java 17** or higher
- **Maven 3.6** or higher
- **Git**


Check with:

```bash
java -version
mvn -version
node -v
npm -v
```

---

## Project Structure
```
src/
└── main/
    └── java/
        └── edu/demo/board/
            ├── BBEndpoint.java         # WebSocket server endpoint
            ├── BBConfigurator.java    # WebSocket configuration
            ├── TicketService.java     # Ticket generation/validation
            ├── BBTicketController.java# REST endpoint for ticket
            ├── SecurityConfig.java    # Spring Security + OAuth2 config
    └── resources/
        └── application.properties     # OAuth2 issuer config
```

## WebSocket Security Protocol

### Why?
WebSocket does not natively support authentication. This project uses a **ticket-based protocol** with Google OAuth2 for secure access.

### Flow
1. **User logs in with Google** on the frontend and obtains an `id_token`.
2. **Frontend requests a ticket** from the backend via `/api/ws-ticket`, sending the `id_token` as a Bearer token.
3. **Backend validates the token** with Google, generates a short-lived ticket, and returns it.
4. **Frontend opens the WebSocket** and sends the ticket as the first message.
5. **Backend validates the ticket** before allowing further communication.

---

## WebSocket Endpoint

- **URL:** `ws://<server-address>/bbService`
- **Protocol:** Standard WebSocket
- **Message format:** JSON
- **Authentication:** First message must be `{ "ticket": "<ticket_value>" }`

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

#### 2. Clear
```json
{
  "type": "clear"
}
```
Clears the canvas for all clients and erases the drawing history from memory

#### 3. Info (sent by server on connection)
```json
{
  "type": "info",
  "message": "Connection established."
}
```

---

## Backend Installation

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
