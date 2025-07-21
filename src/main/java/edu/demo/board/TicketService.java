package edu.demo.board;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TicketService {
    private final Map<String, TicketInfo> tickets = new ConcurrentHashMap<>();

    public String generateTicket(String userId, String clientIp) {
        String ticket = UUID.randomUUID().toString();
        tickets.put(ticket, new TicketInfo(userId, clientIp, Instant.now().plusSeconds(300)));
        return ticket;
    }

    public boolean validateTicket(String ticket, String clientIp) {
        TicketInfo info = tickets.get(ticket);
        if (info == null) return false;
        if (!info.clientIp.equals(clientIp)) return false;
        if (Instant.now().isAfter(info.expiry)) return false;
        tickets.remove(ticket); // Evitar reuso
        return true;
    }

    private static class TicketInfo {
        String userId;
        String clientIp;
        Instant expiry;
        TicketInfo(String userId, String clientIp, Instant expiry) {
            this.userId = userId;
            this.clientIp = clientIp;
            this.expiry = expiry;
        }
    }
} 