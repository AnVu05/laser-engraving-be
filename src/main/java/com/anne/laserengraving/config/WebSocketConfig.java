package com.anne.laserengraving.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Kích hoạt Simple In-Memory Broker cho các topics Pub/Sub
        // - /topic: Broadcast tin nhắn chung (thông báo Job, tiến độ thời gian thực)
        // - /queue: Gửi tin nhắn đích danh cho từng client/thiết bị
        config.enableSimpleBroker("/topic", "/queue");

        // Prefix định tuyến cho các tin nhắn từ Client/Thiết bị gửi lên Controller (@MessageMapping)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint 1: Dành cho bo mạch điều khiển Laser IoT (Makerbase MKS DLC32 V2.1)
        registry.addEndpoint("/ws/device")
                .setAllowedOriginPatterns("*");

        // Endpoint 2: Dành cho ứng dụng Web Studio (React Client) hỗ trợ SockJS fallback
        registry.addEndpoint("/ws/client")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // Endpoint 3: Dành cho ứng dụng Web Studio kết nối Native WebSocket (không qua SockJS)
        registry.addEndpoint("/ws/client")
                .setAllowedOriginPatterns("*");
    }
}
