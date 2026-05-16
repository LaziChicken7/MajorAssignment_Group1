package org.auctionfx.auctionbidsystemspringbootrework.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.auctionfx.auctionbidsystemspringbootrework.dto.request.ChatMessageDTO;
import org.auctionfx.auctionbidsystemspringbootrework.entity.chat.ChatMessage;
import org.auctionfx.auctionbidsystemspringbootrework.entity.chat.Connection;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.User;
import org.auctionfx.auctionbidsystemspringbootrework.enums.NotificationType;
import org.auctionfx.auctionbidsystemspringbootrework.exception.ErrorCode;
import org.auctionfx.auctionbidsystemspringbootrework.exception.UserException;
import org.auctionfx.auctionbidsystemspringbootrework.repository.ChatMessageRepository;
import org.auctionfx.auctionbidsystemspringbootrework.repository.ConnectionRepository;
import org.auctionfx.auctionbidsystemspringbootrework.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper; // Nhớ thêm import này trên đầu file

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ChatService {

    @Autowired private ChatMessageRepository chatRepository;
    @Autowired private ConnectionRepository connectionRepository;
    @Autowired private UserRepository userRepository;

    // Công cụ "bắn" tin nhắn qua WebSocket
    @Autowired private SimpMessagingTemplate messagingTemplate;

    // Xóa @Autowired đi và tự khởi tạo.
    // findAndRegisterModules() giúp nó dịch được kiểu LocalDateTime không bị lỗi!
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired private NotificationService notificationService;

    // =========================================================
    // 1. LOGIC TIN NHẮN (REAL-TIME)
    // =========================================================
    @Transactional
    public void processAndSendMessage(ChatMessageDTO chatMessage) {
        log.info("SERVICE: Xử lý tin nhắn từ [{}] gửi đến [{}]", chatMessage.getSender(), chatMessage.getReceiver());

        User sender = userRepository.findByUserName(chatMessage.getSender());
        User receiver = userRepository.findByUserName(chatMessage.getReceiver());

        if (sender == null || receiver == null) {
            log.error("Lỗi Chat: Không tìm thấy người gửi hoặc người nhận!");
            return;
        }

        // 1. Lưu xuống Database
        ChatMessage savedMsg = new ChatMessage();
        savedMsg.setSender(sender);
        savedMsg.setReceiver(receiver);
        savedMsg.setContent(chatMessage.getContent());
        savedMsg.setSendTime(LocalDateTime.now());
        chatRepository.save(savedMsg);

        log.debug("Đã lưu tin nhắn vào CSDL. Chuẩn bị bắn qua WebSocket...");

        // ====================================================================
        // FIX LỖI LOCALDATETIME & TRÁNH LỖI VÒNG LẶP ENTITY DATABASE
        // ====================================================================
        try {
            // 1. Tạo một Map (giống y hệt cấu trúc ChatMessageModel bên JavaFX)
            java.util.Map<String, Object> responseMsg = new java.util.HashMap<>();
            responseMsg.put("id", savedMsg.getId());
            responseMsg.put("content", savedMsg.getContent());
            responseMsg.put("isRead", savedMsg.isRead());

            // BIẾN THỜI GIAN THÀNH CHUỖI STRING ĐỂ TRÁNH LỖI JACKSON
            responseMsg.put("sendTime", savedMsg.getSendTime().toString());

            // 2. Lấy đúng 3 thông tin cần thiết của Sender (Không lấy Password hay dữ liệu thừa)
            java.util.Map<String, String> senderInfo = new java.util.HashMap<>();
            senderInfo.put("userName", sender.getUserName());
            senderInfo.put("fullName", sender.getFullName());
            senderInfo.put("avatarUrl", sender.getAvatarUrl());
            responseMsg.put("sender", senderInfo);

            // 3. Lấy thông tin Receiver
            java.util.Map<String, String> receiverInfo = new java.util.HashMap<>();
            receiverInfo.put("userName", receiver.getUserName());
            receiverInfo.put("fullName", receiver.getFullName());
            receiverInfo.put("avatarUrl", receiver.getAvatarUrl());
            responseMsg.put("receiver", receiverInfo);

            // 4. Ép Map này thành chuỗi JSON chuẩn
            String jsonPayload = objectMapper.writeValueAsString(responseMsg);

            // 5. Bắn đi qua WebSocket
            messagingTemplate.convertAndSend("/topic/messages/" + chatMessage.getReceiver(), jsonPayload);
            messagingTemplate.convertAndSend("/topic/messages/" + chatMessage.getSender(), jsonPayload);

            log.info("Bắn tin nhắn thành công qua WebSocket!");

        } catch (Exception e) {
            log.error("Lỗi chuyển đổi JSON khi gửi qua WebSocket: ", e);
        }
    }

    public List<ChatMessage> getChatHistory(String senderUsername, String receiverUsername) {
        log.info("SERVICE: Lấy lịch sử trò chuyện giữa [{}] và [{}]", senderUsername, receiverUsername);
        User u1 = userRepository.findByUserName(senderUsername);
        User u2 = userRepository.findByUserName(receiverUsername);

        if (u1 == null || u2 == null) {
            throw new UserException(ErrorCode.USER_NOT_FOUND);
        }
        return chatRepository.findChatHistory(u1, u2);
    }

    // =========================================================
    // 2. LOGIC DANH BẠ KẾT BẠN (CONNECTIONS)
    // =========================================================

    @Transactional
    public String sendFriendRequest(String senderUsername, String receiverUsername) {
        log.info("SERVICE: [{}] gửi yêu cầu kết bạn đến [{}]", senderUsername, receiverUsername);
        User sender = userRepository.findByUserName(senderUsername);
        User receiver = userRepository.findByUserName(receiverUsername);

        if (sender == null || receiver == null) throw new UserException(ErrorCode.USER_NOT_FOUND);

        Connection existConn = connectionRepository.findExistingConnection(sender, receiver);
        if (existConn != null) {
            throw new RuntimeException("Hai người đã có liên kết từ trước (Bạn bè hoặc Đang chờ)!");
        }

        Connection conn = new Connection();
        conn.setSender(sender);
        conn.setReceiver(receiver);
        conn.setStatus("PENDING");
        connectionRepository.save(conn);

        // =========================================================================
        // BỔ SUNG: BẮN THÔNG BÁO VÀO HÒM THƯ NGƯỜI NHẬN
        // Phải tuân thủ đúng Cú pháp Title để lúc Accept nó cắt chuỗi ra lấy Username
        // =========================================================================
        String title = "Yêu cầu kết bạn từ: " + sender.getUserName();
        String desc = sender.getFullName() + " muốn kết bạn với bạn để trò chuyện.";
        notificationService.createNotification(receiver, null, NotificationType.FRIEND_REQUEST, title, desc);

        return "Đã gửi yêu cầu kết bạn!";
    }

    @Transactional
    public String acceptFriendRequest(String senderUsername, String receiverUsername) {
        log.info("SERVICE: [{}] đồng ý kết bạn với [{}]", receiverUsername, senderUsername);
        User sender = userRepository.findByUserName(senderUsername);
        User receiver = userRepository.findByUserName(receiverUsername);

        Connection existConn = connectionRepository.findExistingConnection(sender, receiver);
        if (existConn == null || !"PENDING".equals(existConn.getStatus())) {
            throw new RuntimeException("Không tìm thấy lời mời kết bạn hợp lệ!");
        }

        existConn.setStatus("ACCEPTED");
        connectionRepository.save(existConn);
        return "Kết bạn thành công! Bây giờ hai bạn có thể trò chuyện.";
    }

    public List<Connection> getFriendsList(String username) {
        log.info("SERVICE: Lấy danh sách bạn bè của [{}]", username);
        User user = userRepository.findByUserName(username);
        if (user == null) throw new UserException(ErrorCode.USER_NOT_FOUND);

        return connectionRepository.findAcceptedConnectionsByUser(user);
    }

    // =========================================================
    // HÀM MỚI: KIỂM TRA TRẠNG THÁI MỐI QUAN HỆ GIỮA 2 NGƯỜI
    // =========================================================
    public String checkConnectionStatus(String user1, String user2) {
        // 1. Tìm User entity từ Username (Sử dụng đúng hàm findByUserName của bạn)
        User u1 = userRepository.findByUserName(user1);
        User u2 = userRepository.findByUserName(user2);

        if (u1 == null || u2 == null) {
            return "NONE";
        }

        // 2. Query trong DB xem có Connection nào giữa 2 User này không
        // (Sử dụng đúng hàm findExistingConnection truyền vào 2 đối tượng User)
        Connection conn = connectionRepository.findExistingConnection(u1, u2);

        if (conn == null) {
            return "NONE";
        }

        // 3. Lấy trạng thái (status của bạn đã là String nên không cần .name() nữa)
        String status = conn.getStatus();

        if ("ACCEPTED".equals(status)) {
            return "ACCEPTED";
        }

        if ("PENDING".equals(status)) {
            // 4. Phân biệt ai là người gửi để Frontend biết đường hiển thị
            if (conn.getSender().getId().equals(u1.getId())) {
                return "PENDING_SENDER";   // user1 là người gửi -> Hiện nút "Đã gửi lời mời"
            } else {
                return "PENDING_RECEIVER"; // user2 là người gửi -> Hiện nút "Chấp nhận/Từ chối"
            }
        }

        return "NONE";
    }

    @Transactional
    public String declineOrRemoveConnection(String senderUsername, String receiverUsername) {
        log.info("SERVICE: [{}] thực hiện xóa kết nối với [{}]", receiverUsername, senderUsername);

        User user1 = userRepository.findByUserName(senderUsername);
        User user2 = userRepository.findByUserName(receiverUsername);

        if (user1 == null || user2 == null) {
            throw new UserException(ErrorCode.USER_NOT_FOUND);
        }

        // Tìm connection giữa 2 người (không phân biệt ai gửi trước)
        Connection existConn = connectionRepository.findExistingConnection(user1, user2);

        if (existConn == null) {
            throw new RuntimeException("Không tìm thấy yêu cầu kết bạn hoặc liên kết bạn bè!");
        }

        // Xóa bản ghi khỏi database
        connectionRepository.delete(existConn);

        // (Tùy chọn) Nếu muốn chuyên nghiệp hơn, bạn có thể tạo thông báo cho người kia
        // notificationService.createNotification(user1, null, NotificationType.SYSTEM, "Thông báo", user2.getFullName() + " đã từ chối/hủy kết bạn.");

        return "Đã xóa yêu cầu kết bạn hoặc hủy kết bạn thành công.";
    }
}