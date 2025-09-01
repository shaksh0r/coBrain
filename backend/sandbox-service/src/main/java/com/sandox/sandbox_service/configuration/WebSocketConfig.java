package com.sandox.sandbox_service.configuration;

import com.sandox.sandbox_service.service.CodeExecution;
import com.sandox.sandbox_service.service.GdbDebugger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private GdbDebugger gdbDebugger;
    //private CodeExecution codeExecution;
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gdbDebugger, "/ws").setAllowedOrigins("*");
    }
}
