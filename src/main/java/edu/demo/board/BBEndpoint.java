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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket endpoint for the collaborative drawing board application.
 * This endpoint manages real-time drawing events from connected clients.
 * It authenticates clients using one-time tickets, maintains an in-memory draw history,
 * and broadcasts drawing events to all connected clients.
 * Supported message types:
 * "draw": Draw event with coordinates, color, and size.
 *  "clear": Clears the canvas and resets the history.
 *
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
     * Set de sesiones autenticadas para chat en vivo
     */
    private static final Set<Session> authenticatedSessions = ConcurrentHashMap.newKeySet();

    /**
     * Called when a new WebSocket connection is established.
     * Adds the session to the queue. Draw history is sent only after authentication.
     *
     * @param session the session representing the new client connection
     * @param config the endpoint configuration
     */
    @OnOpen
    public void openConnection(Session session, EndpointConfig config) {
        queue.add(session);
        ownSession = session;
        logger.log(Level.INFO, "Connection opened.");
        // No enviar historial hasta que esté autenticado
    }

    /**
     * Handles incoming messages from clients.
     * If not authenticated, expects a ticket as the first message and authenticates the session.
     * If authenticated, processes "draw" and "clear" events, updates history, and broadcasts to others.
     *
     * @param message the message received (expected to be a JSON string)
     * @param session the session from which the message originated
     */
    @OnMessage
    public void processMessage(String message, Session session) {
        if (!authenticated) {
            handleAuthentication(message, session);
            return;
        }

        if (isChatMessage(message)) {
            sendChatToAuthenticated(message, session);
            return;
        }

        handleDrawingMessage(message);
        sendToOthers(message);
    }
    private void handleAuthentication(String message, Session session) {
        String ticket = extraerTicket(message);
        String clientIp = session.getRequestURI().getHost(); // Ajusta si es necesario

        if (ticketService != null && ticketService.validateTicket(ticket, clientIp)) {
            authenticated = true;
            authenticatedSessions.add(session);
            sendDrawHistory(session);
            sendInfoMessage(session, "Authenticated.");
        } else {
            closeSessionWithPolicyViolation(session, "Invalid ticket");
        }
    }

    private void sendDrawHistory(Session session) {
        System.out.println("Enviando historial de dibujo: " + drawHistory.size() + " eventos");
        for (String event : drawHistory) {
            try {
                System.out.println("Enviando evento: " + event);
                session.getBasicRemote().sendText(event);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error sending past message", e);
            }
        }
    }

    private void sendInfoMessage(Session session, String message) {
        try {
            session.getBasicRemote().sendText("{\"type\":\"info\",\"message\":\"" + message + "\"}");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error sending connection message", e);
        }
    }

    private void closeSessionWithPolicyViolation(Session session, String reason) {
        try {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, reason));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error closing session", e);
        }
    }

    private boolean isChatMessage(String message) {
        return message.contains("\"type\":\"chat\"");
    }

    private void handleDrawingMessage(String message) {
        System.out.println("Message received: " + message);

        if (message.contains("\"type\":\"clear\"")) {
            drawHistory.clear();
        } else {
            drawHistory.add(message);
        }
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
        authenticatedSessions.remove(session);
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
        authenticatedSessions.remove(session);
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

    /**
     * Envía un mensaje de chat solo a los usuarios autenticados (excepto el emisor)
     */
    private void sendChatToAuthenticated(String msg, Session sender) {
        for (Session s : authenticatedSessions) {
            if (!s.equals(sender)) {
                try {
                    s.getBasicRemote().sendText(msg);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Error sending chat message", e);
                }
            }
        }
    }

    /**
     * Extracts the ticket value from the initial JSON message.
     *
     * @param message the JSON message containing the ticket
     * @return the extracted ticket string, or null if not found
     */
    private String extraerTicket(String message) {
        try {
            int start = message.indexOf("\"ticket\":\"");
            if (start != -1) {
                int end = message.indexOf("\"", start + 10);
                if (end != -1) {
                    return message.substring(start + 10, end);
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error extracting ticket from message", e);
        }
        return null;
    }
    /**
     *
     */
    public static void clearDrawHistory() {
        drawHistory.clear();
    }
    public static void addDrawHistory(String event) {
        drawHistory.add(event);
    }
    public static boolean containsDrawHistory(String event) {
        return drawHistory.contains(event);
    }
    public static boolean isDrawHistoryEmpty() {
        return drawHistory.isEmpty();
    }
    public static void clearQueue() {
        queue.clear();
    }

    public static void addToQueue(Session session) {
        queue.add(session);
    }

    public static boolean queueContains(Session session) {
        return queue.contains(session);
    }
}
