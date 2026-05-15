package org.auctionfx.auctionbidsystemspringbootrework.repository;

import org.auctionfx.auctionbidsystemspringbootrework.entity.chat.ChatMessage;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {

    // Lấy toàn bộ lịch sử tin nhắn của 2 người, sắp xếp theo thời gian cũ -> mới
    @Query("SELECT m FROM ChatMessage m WHERE (m.sender = :u1 AND m.receiver = :u2) OR (m.sender = :u2 AND m.receiver = :u1) ORDER BY m.sendTime ASC")
    List<ChatMessage> findChatHistory(@Param("u1") User u1, @Param("u2") User u2);
}