package edu.demo.board;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/ws-ticket")
//@CrossOrigin("*")
public class BBTicketController {

    private final TicketService ticketService;

    public BBTicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<?> getTicket(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request) {
        String userId = jwt.getSubject(); // o jwt.getClaim("email") si prefieres el email
        String clientIp = request.getRemoteAddr();
        String ticket = ticketService.generateTicket(userId, clientIp);
        System.out.println(ticket + " " + clientIp);
        return ResponseEntity.ok(Map.of("ticket", ticket));
    }
} 