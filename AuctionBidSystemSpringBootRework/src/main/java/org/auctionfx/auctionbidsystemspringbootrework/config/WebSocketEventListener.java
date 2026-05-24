package org.auctionfx.auctionbidsystemspringbootrework.config;

import lombok.extern.slf4j.Slf4j;
import org.auctionfx.auctionbidsystemspringbootrework.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@Slf4j
public class WebSocketEventListener {

    @Autowired
    private UserService userService;

    // BẮT SỰ KIỆN KHI JAVAFX BẬT LÊN
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        // Đọc cái "username" cấu hình gửi lên từ JavaFX
        String username = accessor.getFirstNativeHeader("username");

        if (username != null) {
            // Cất vào Session để lát nữa dùng khi họ thoát
            accessor.getSessionAttributes().put("username", username);
            // Báo cho UserService biết là nó đã Online
            userService.setUserOnline(username);
        }
    }

    // BẮT SỰ KIỆN KHI JAVAFX TẮT ĐI / MẤT MẠNG
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        // Lấy lại username từ Session
        String username = (String) accessor.getSessionAttributes().get("username");

        if (username != null) {
            // Báo cho UserService biết là nó đã Offline
            userService.setUserOffline(username);
        }
    }
}