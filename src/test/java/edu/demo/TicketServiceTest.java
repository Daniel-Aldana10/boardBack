package edu.demo;

import edu.demo.board.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TicketServiceTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ticketService = new TicketService(redisTemplate);
    }

    @Test
    void testGenerateTicketStoresTicketInRedis() {
        String userId = "user123";
        String clientIp = "127.0.0.1";
        String ticket = ticketService.generateTicket(userId, clientIp);

        assertNotNull(ticket);
        verify(valueOperations, times(1)).set(eq(ticket), eq(userId + ":" + clientIp), eq(5L), eq(java.util.concurrent.TimeUnit.MINUTES));
    }

    @Test
    void testValidateTicketReturnsTrueAndDeletesTicket() {
        String ticket = "ticket-abc";
        String clientIp = "127.0.0.1";
        when(valueOperations.get(ticket)).thenReturn("user123:" + clientIp);

        boolean result = ticketService.validateTicket(ticket, clientIp);

        assertTrue(result);
        verify(redisTemplate, times(1)).delete(ticket);
    }

    @Test
    void testValidateTicketReturnsFalseIfTicketNotFound() {
        String ticket = "ticket-xyz";
        String clientIp = "127.0.0.1";
        when(valueOperations.get(ticket)).thenReturn(null);

        boolean result = ticketService.validateTicket(ticket, clientIp);

        assertFalse(result);
        verify(redisTemplate, never()).delete(ticket);
    }

    @Test
    void testValidateTicketReturnsFalseIfFormatInvalid() {
        String ticket = "ticket-abc";
        String clientIp = "127.0.0.1";
        when(valueOperations.get(ticket)).thenReturn("invalidformat");

        boolean result = ticketService.validateTicket(ticket, clientIp);

        assertTrue(result);

    }
}