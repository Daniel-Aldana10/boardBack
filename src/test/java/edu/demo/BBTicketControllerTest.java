package edu.demo;

import edu.demo.board.BBTicketController;
import edu.demo.board.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import jakarta.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;

class BBTicketControllerTest {

    @Test
    void testGetTicketReturnsTicket() {
        TicketService ticketService = mock(TicketService.class);
        BBTicketController controller = new BBTicketController(ticketService);

        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("user123");

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        when(ticketService.generateTicket("user123", "127.0.0.1")).thenReturn("ticket-abc");

        ResponseEntity<?> response = controller.getTicket(jwt, request);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody() instanceof Map);
        assertEquals("ticket-abc", ((Map<?, ?>) response.getBody()).get("ticket"));
    }
}