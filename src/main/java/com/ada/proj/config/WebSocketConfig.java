package com.ada.proj.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final CorsProperties corsProperties;

    public WebSocketConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트 구독 prefix
        registry.enableSimpleBroker("/topic");
        // 서버로 메시지 보낼 prefix
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        java.util.List<String> allowed = corsProperties.getAllowedOriginPatterns();
        String[] origins = (allowed == null || allowed.isEmpty())
                ? new String[]{"http://localhost:*", "http://127.0.0.1:*"}
                : allowed.toArray(String[]::new);

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(origins)
                .withSockJS();
    }
}
