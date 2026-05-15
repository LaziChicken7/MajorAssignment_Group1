package org.auctionfx.auctionbidsystemspringbootrework.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // Bật tính năng Tổng đài tin nhắn
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Mở cổng kết nối: JavaFX sẽ dùng link "ws://localhost:8080/ws-chat" để cắm cáp vào Server
        // setAllowedOriginPatterns("*") để không bị chặn bảo mật (CORS)
        registry.addEndpoint("/ws-chat").setAllowedOriginPatterns("*");

        // Thêm một cổng dự phòng hỗ trợ SockJS (Dành cho bản Web nếu sau này bạn làm Web)
        registry.addEndpoint("/ws-chat").setAllowedOriginPatterns("*").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // ========================================================
        // FIX: Đổi từ /user sang /topic để hỗ trợ kết nối nặc danh
        // ========================================================
        registry.enableSimpleBroker("/topic");

        registry.setApplicationDestinationPrefixes("/app");
        // Xóa dòng registry.setUserDestinationPrefix("/user"); đi luôn cũng được
    }
}