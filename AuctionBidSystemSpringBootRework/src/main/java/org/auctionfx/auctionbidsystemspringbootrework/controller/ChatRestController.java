package org.auctionfx.auctionbidsystemspringbootrework.controller;

import org.auctionfx.auctionbidsystemspringbootrework.dto.response.ApiResponse;
import org.auctionfx.auctionbidsystemspringbootrework.entity.chat.ChatMessage;
import org.auctionfx.auctionbidsystemspringbootrework.entity.chat.Connection;
import org.auctionfx.auctionbidsystemspringbootrework.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
public class ChatRestController {

    @Autowired
    private ChatService chatService;

    // ============================================
    // API LẤY LỊCH SỬ CHAT
    // ============================================
    @GetMapping("/history")
    public ApiResponse<List<ChatMessage>> getChatHistory(@RequestParam String user1, @RequestParam String user2) {
        ApiResponse<List<ChatMessage>> response = new ApiResponse<>();
        response.setResult(chatService.getChatHistory(user1, user2));
        return response;
    }

    // ============================================
    // API QUẢN LÝ KẾT BẠN (CONNECTIONS)
    // ============================================

    @PostMapping("/friend-request")
    public ApiResponse<String> sendFriendRequest(@RequestParam String sender, @RequestParam String receiver) {
        ApiResponse<String> response = new ApiResponse<>();
        response.setResult(chatService.sendFriendRequest(sender, receiver));
        return response;
    }

    @PutMapping("/accept-request")
    public ApiResponse<String> acceptFriendRequest(@RequestParam String sender, @RequestParam String receiver) {
        ApiResponse<String> response = new ApiResponse<>();
        response.setResult(chatService.acceptFriendRequest(sender, receiver));
        return response;
    }

    @GetMapping("/friends")
    public ApiResponse<List<Connection>> getFriendsList(@RequestParam String username) {
        ApiResponse<List<Connection>> response = new ApiResponse<>();
        response.setResult(chatService.getFriendsList(username));
        return response;
    }

    // ============================================
    // API KIỂM TRA TRẠNG THÁI BẠN BÈ
    // ============================================
    @GetMapping("/check-connection")
    public ApiResponse<String> checkConnectionStatus(@RequestParam String user1, @RequestParam String user2) {
        ApiResponse<String> response = new ApiResponse<>();
        response.setResult(chatService.checkConnectionStatus(user1, user2));
        return response;
    }

    // ============================================
    // API TỪ CHỐI HOẶC HỦY KẾT BẠN
    // ============================================
    @DeleteMapping("/decline-request")
    public ApiResponse<String> declineFriendRequest(@RequestParam String sender, @RequestParam String receiver) {
        ApiResponse<String> response = new ApiResponse<>();
        response.setResult(chatService.declineOrRemoveConnection(sender, receiver));
        return response;
    }
}