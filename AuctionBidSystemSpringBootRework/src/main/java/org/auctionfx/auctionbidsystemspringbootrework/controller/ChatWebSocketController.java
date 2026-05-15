package org.auctionfx.auctionbidsystemspringbootrework.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.auctionfx.auctionbidsystemspringbootrework.dto.request.ChatMessageDTO;
import org.auctionfx.auctionbidsystemspringbootrework.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
public class ChatWebSocketController {

    @Autowired
    private ChatService chatService;

    // Khởi tạo ObjectMapper để tự dịch JSON
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MessageMapping("/chat.send")
    // FIX: Nhận trực tiếp chuỗi String JSON từ JavaFX thay vì bắt Spring tự ép kiểu sang DTO
    public void processMessage(@Payload String jsonPayload) {
        try {
            // Tự dùng ObjectMapper để chuyển đổi chuỗi JSON thành Object ChatMessageDTO
            ChatMessageDTO chatMessage = objectMapper.readValue(jsonPayload, ChatMessageDTO.class);

            // Xử lý logic như bình thường
            chatService.processAndSendMessage(chatMessage);

        } catch (Exception e) {
            log.error("LỖI WEBSOCKET (Không thể dịch JSON): {}", e.getMessage());
            e.printStackTrace();
        }
    }
}