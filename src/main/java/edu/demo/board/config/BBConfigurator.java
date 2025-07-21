package edu.demo.board.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket configuration class.
 * Enables scheduling and registers WebSocket endpoints annotated with @ServerEndpoint.
 * Exposes the ServerEndpointExporter bean required to enable WebSocket support
 * with embedded servlet containers like Tomcat.
 */
@Configuration
@EnableScheduling
public class BBConfigurator {

    /**
     * Exposes the ServerEndpointExporter bean required to enable WebSocket support.
     *
     * @return a ServerEndpointExporter bean
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
