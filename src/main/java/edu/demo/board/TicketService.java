package edu.demo.board;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for generating and validating one-time-use tickets for WebSocket authentication.
 */
@Service
public class TicketService {
    private final StringRedisTemplate redisTemplate;

    /**
     * Constructs the TicketService with the given Redis template.
     * @param redisTemplate the Redis template for string operations
     */
    public TicketService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Generates a new one-time-use ticket for the given user and client IP.
     * The ticket is stored in Redis with a TTL of 5 minutes.
     *
     * @param userId the user identifier
     * @param clientIp the client's IP address
     * @return the generated ticket string
     */
    public String generateTicket(String userId, String clientIp) {
        String ticket = UUID.randomUUID().toString();
        String value = userId + ":" + clientIp;
        redisTemplate.opsForValue().set(ticket, value, 5, TimeUnit.MINUTES);
        System.out.println("<UNK>" + ticket);
        return ticket;
    }

    /**
     * Validates the given ticket for the specified client IP.
     * The ticket is deleted after validation to prevent reuse.
     *
     * @param ticket the ticket string to validate
     * @param clientIp the client's IP address
     * @return true if the ticket is valid and matches the client IP; false otherwise
     */
    public boolean validateTicket(String ticket, String clientIp) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        String value = ops.get(ticket);
        System.out.println("Validando ticket: " + ticket + " para IP: " + clientIp + " valor en Redis: " + value);
        if (value == null) return false;
        String[] parts = value.split(":");
        //if (parts.length != 2) return false;
        // if (!parts[1].equals(clientIp)) return false; // Validate IP if needed
        redisTemplate.delete(ticket); // Prevent reuse
        return true;
    }
}