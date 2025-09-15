package com.sandox.sandbox_service.configuration;

import com.sandox.sandbox_service.service.cpp.CppCodeExecution;
import com.sandox.sandbox_service.service.cpp.GdbDebugger;
import com.sandox.sandbox_service.service.java.JavaCodeExecution;
import com.sandox.sandbox_service.service.java.JdbDebugger;
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
    @Autowired
    private CppCodeExecution cppCodeExecution;
    @Autowired
    private JavaCodeExecution javaCodeExecution;
    @Autowired
    private JdbDebugger jdbDebugger;
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(cppCodeExecution, "/cpp").setAllowedOrigins("*");
        registry.addHandler(gdbDebugger,"/debugCpp").setAllowedOrigins("*");
        registry.addHandler(javaCodeExecution, "/java").setAllowedOrigins("*");
        registry.addHandler(jdbDebugger,"/debugJava").setAllowedOrigins("*");
    }
}
