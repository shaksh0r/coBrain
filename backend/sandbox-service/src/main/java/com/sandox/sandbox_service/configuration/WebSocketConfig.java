package com.sandox.sandbox_service.configuration;

import com.sandox.sandbox_service.controller.MyWebSocketHandler;
import com.sandox.sandbox_service.service.CodeExecution;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new CodeExecution(), "/ws").setAllowedOrigins("*");
    }
}
