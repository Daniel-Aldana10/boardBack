package edu.demo.board;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * REST controller for issuing WebSocket authentication tickets.
 */
@RestController
@RequestMapping("/api/ws-ticket")
public class BBTicketController {

    private final TicketService ticketService;

    /**
     * Constructs the controller with the given TicketService.
     * @param ticketService the ticket service
     */
    public BBTicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * Issues a new one-time-use ticket for WebSocket authentication.
     *
     * @param jwt the authenticated user's JWT
     * @param request the HTTP servlet request (to get client IP)
     * @return a map containing the generated ticket
     */
    @PostMapping
    public ResponseEntity<?> getTicket(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request) {
        String userId = jwt.getSubject();
        String clientIp = request.getRemoteAddr();
        String ticket = ticketService.generateTicket(userId, clientIp);
        System.out.println(ticket + " " + clientIp);
        return ResponseEntity.ok(Map.of("ticket", ticket));
    }
} 