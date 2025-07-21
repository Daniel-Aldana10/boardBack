package edu.demo.board;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WebSocket endpoint for a collaborative drawing board.
 *
 * This server handles real-time drawing updates from connected clients.
 * It maintains an in-memory list of draw events and broadcasts them to all clients.
 *
 * Supported message types:
 * - "draw": Draw event with coordinates, color, and size.
 * - "clear": Clears the canvas and resets the history.
 */
@Component
@ServerEndpoint("/bbService")
public class BBEndpoint {

    private static final Logger logger = Logger.getLogger(BBEndpoint.class.getName());

    /**
     * Queue of currently connected WebSocket sessions.
     */
    static Queue<Session> queue = new ConcurrentLinkedQueue<>();

    /**
     * The WebSocket session for the current connection.
     */
    private Session ownSession;

    /**
     * In-memory list storing the draw history as JSON strings.
     * This is shared across all sessions.
     */
    static List<String> drawHistory = new ArrayList<>();

    private static TicketService ticketService;

    @Autowired
    public void setTicketService(TicketService service) {
        BBEndpoint.ticketService = service;
    }

    /**
     * Almacena si la sesión ya está autenticada
     */
    private boolean authenticated = false;

    /**
     * Called when a new WebSocket connection is established.
     * Sends the full draw history to the new client and adds the session to the queue.
     *
     * @param session the session representing the new client connection
     */
    @OnOpen
    public void openConnection(Session session, EndpointConfig config) {
        queue.add(session);
        ownSession = session;
        logger.log(Level.INFO, "Connection opened.");
        // No enviar historial hasta que esté autenticado
    }

    /**
     * Called when a message is received from a client.
     * If it's a "draw" event, it adds it to the history.
     * If it's a "clear" event, it clears the entire history.
     * In both cases, the message is broadcast to all other clients.
     *
     * @param message the message received (expected to be a JSON string)
     * @param session the session from which the message originated
     */
    @OnMessage
    public void processMessage(String message, Session session) {
        if (!authenticated) {
            // Espera el ticket como primer mensaje
            String ticket = extraerTicket(message);
            String clientIp = session.getRequestURI().getHost(); // Puede requerir ajuste según despliegue
            if (ticketService != null && ticketService.validateTicket(ticket, clientIp)) {
                authenticated = true;
                // Enviar historial tras autenticación
                for (String event : drawHistory) {
                    try {
                        session.getBasicRemote().sendText(event);
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Error sending past message", e);
                    }
                }
                try {
                    session.getBasicRemote().sendText("{\"type\":\"info\",\"message\":\"Authenticated.\"}");
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, "Error sending connection message", ex);
                }
            } else {
                try {
                    session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Invalid ticket"));
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error closing session", e);
                }
            }
            return;
        }
        System.out.println("Message received: " + message);

        if (message.contains("\"type\":\"clear\"")) {
            // Clear the entire drawing history
            drawHistory.clear();
        } else {
            // Save the draw event in memory
            drawHistory.add(message);
        }

        // Broadcast to other clients
        sendToOthers(message);
    }

    /**
     * Called when a WebSocket connection is closed.
     * Removes the session from the queue.
     *
     * @param session the session that was closed
     */
    @OnClose
    public void closedConnection(Session session) {
        queue.remove(session);
        logger.log(Level.INFO, "Connection closed.");
    }

    /**
     * Called when an error occurs in a WebSocket session.
     * Removes the session from the queue and logs the error.
     *
     * @param session the session that encountered the error
     * @param t the exception thrown
     */
    @OnError
    public void error(Session session, Throwable t) {
        queue.remove(session);
        logger.log(Level.SEVERE, "Connection error.", t);
    }

    /**
     * Sends a message to all connected clients except the sender.
     *
     * @param msg the message to broadcast
     */
    private void sendToOthers(String msg) {
        for (Session session : queue) {
            if (!session.equals(this.ownSession)) {
                try {
                    session.getBasicRemote().sendText(msg);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Error sending message", e);
                }
            }
        }
    }

    private String extraerTicket(String message) {
        try {
            // Assuming the message is a JSON string like {"ticket": "your_ticket_here"}
            // This is a placeholder. In a real application, you'd parse the JSON.
            // For now, we'll just extract the ticket part.
            int start = message.indexOf("\"ticket\":\"");
            if (start != -1) {
                int end = message.indexOf("\"", start + 10); // Assuming ticket is 10 chars long
                if (end != -1) {
                    return message.substring(start + 10, end);
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error extracting ticket from message", e);
        }
        return null;
    }
}
