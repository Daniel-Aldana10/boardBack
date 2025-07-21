package edu.demo;


import edu.demo.board.BBEndpoint;
import edu.demo.board.TicketService;
import jakarta.websocket.CloseReason;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.io.IOException;
import java.net.URI;

import static org.mockito.Mockito.*;
import org.mockito.InOrder;

public class BBEndpointTest {

    @Mock
    private Session session;

    @Mock
    private RemoteEndpoint.Basic remote;

    @Mock
    private TicketService ticketService;

    @InjectMocks
    private BBEndpoint bbEndpoint;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        BBEndpoint.clearDrawHistory();
        BBEndpoint.clearQueue();
        bbEndpoint = new BBEndpoint();
        bbEndpoint.setTicketService(ticketService);

        when(session.getBasicRemote()).thenReturn(remote);
        when(session.getRequestURI()).thenReturn(URI.create("ws://localhost:8080/bbService"));
    }

    @Test
    void testOpenConnection() {
        bbEndpoint.openConnection(session, null);
        assert BBEndpoint.queueContains(session);
    }

    @Test
    void testAuthenticatedMessageSendsHistory() throws IOException {
        
        BBEndpoint.addDrawHistory("{\"type\":\"draw\",\"x\":1,\"y\":2}");
        BBEndpoint.addDrawHistory("{\"type\":\"draw\",\"x\":3,\"y\":4}");

        when(ticketService.validateTicket("valid-ticket", "localhost")).thenReturn(true);

        bbEndpoint.processMessage("{\"ticket\":\"valid-ticket\"}", session);


        InOrder inOrder = inOrder(remote);
        inOrder.verify(remote).sendText("{\"type\":\"draw\",\"x\":1,\"y\":2}");
        inOrder.verify(remote).sendText("{\"type\":\"draw\",\"x\":3,\"y\":4}");
        inOrder.verify(remote).sendText("{\"type\":\"info\",\"message\":\"Authenticated.\"}");
    }



    @Test
    void testInvalidTicketClosesSession() throws IOException {
        when(ticketService.validateTicket("invalid", "localhost")).thenReturn(false);

        String ticketJson = "{\"ticket\":\"invalid\"}";
        bbEndpoint.processMessage(ticketJson, session);

        verify(session).close(any(CloseReason.class));
    }

    @Test
    void testInvalidJsonTicket() throws IOException {

        bbEndpoint.processMessage("{\"foo\":\"bar\"}", session);

        verify(session).close(any(CloseReason.class));
    }

    @Test
    void testMalformedJsonDoesNotThrow() {
        Assertions.assertDoesNotThrow(() -> bbEndpoint.processMessage("{malformed}", session));
    }

    @Test
    void testClearMessageAfterAuth() throws IOException {
        when(ticketService.validateTicket("valid-ticket", "localhost")).thenReturn(true);
        bbEndpoint.processMessage("{\"ticket\":\"valid-ticket\"}", session);

        BBEndpoint.addDrawHistory("{\"type\":\"draw\",\"x\":1,\"y\":2}");
        bbEndpoint.processMessage("{\"type\":\"clear\"}", session);

        Assertions.assertTrue(BBEndpoint.isDrawHistoryEmpty());
    }

    @Test
    void testDrawMessageAfterAuth() throws IOException {
        when(ticketService.validateTicket("valid-ticket", "localhost")).thenReturn(true);
        bbEndpoint.processMessage("{\"ticket\":\"valid-ticket\"}", session);

        String drawMessage = "{\"type\":\"draw\",\"x\":1,\"y\":2}";
        bbEndpoint.processMessage(drawMessage, session);

        Assertions.assertTrue(BBEndpoint.containsDrawHistory(drawMessage));
    }

    @Test
    void testOpenAndClosedConnection() {
        bbEndpoint.openConnection(session, null);
        Assertions.assertTrue(BBEndpoint.queueContains(session));
        bbEndpoint.closedConnection(session);
        Assertions.assertFalse(BBEndpoint.queueContains(session));
    }

    @Test
    void testErrorRemovesSession() {
        bbEndpoint.openConnection(session, null);
        bbEndpoint.error(session, new Exception("Simulated error"));
        Assertions.assertFalse(BBEndpoint.queueContains(session));
    }
}
