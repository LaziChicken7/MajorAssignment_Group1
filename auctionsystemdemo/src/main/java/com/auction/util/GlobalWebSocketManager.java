package com.auction.util;

import com.auction.model.ChatMessageModel;
import javafx.application.Platform;

import java.util.function.Consumer;

public class GlobalWebSocketManager {

    private static WebSocketClientService webSocketService;

    // Nơi để ChatController đăng ký nhận tin nhắn khi đang mở trang Chat
    private static Consumer<ChatMessageModel> activeChatListener = null;

    // Biến lưu trữ xem người dùng đang chat với ai (để không hiện thông báo nếu đang mở khung chat của người đó)
    public static String currentActiveChatPartner = null;

    public static void initConnection() {
        if (webSocketService == null && SessionManager.userName != null) {
            webSocketService = new WebSocketClientService();
            webSocketService.connect(SessionManager.userName, GlobalWebSocketManager::handleIncomingMessage);
        }
    }

    public static void sendMessage(String sender, String receiver, String content) {
        if (webSocketService != null) {
            webSocketService.sendMessage(sender, receiver, content);
        }
    }

    public static void setActiveChatListener(Consumer<ChatMessageModel> listener) {
        activeChatListener = listener;
    }

    private static void handleIncomingMessage(String jsonPayload) {
        System.out.println("📩 GLOBAL STOMP NHẬN ĐƯỢC: " + jsonPayload);

        try {
            String cleanJson = jsonPayload.trim();
            ChatMessageModel msg = null;

            if (cleanJson.startsWith("[")) {
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<ChatMessageModel>>(){}.getType();
                java.util.List<ChatMessageModel> list = ApiService.gson.fromJson(cleanJson, listType);
                if (list != null && !list.isEmpty()) msg = list.get(0);
            } else {
                msg = ApiService.gson.fromJson(cleanJson, ChatMessageModel.class);
            }

            if (msg != null) {
                final ChatMessageModel finalMsg = msg;
                Platform.runLater(() -> {
                    // Nếu tôi là người nhận tin nhắn (người khác nhắn cho tôi)
                    if (finalMsg.receiver.userName.equals(SessionManager.userName)) {

                        // Kịch bản 1: Đang mở đúng trang chat của người này -> Ném vào UI Chat, KHÔNG HIỆN TOAST
                        if (finalMsg.sender.userName.equals(currentActiveChatPartner) && activeChatListener != null) {
                            activeChatListener.accept(finalMsg);
                        }
                        // Kịch bản 2: Đang ở trang khác HOẶC đang chat với người khác -> HIỆN TOAST GLOBAL
                        else {
                            String senderName = finalMsg.sender.fullName != null ? finalMsg.sender.fullName : finalMsg.sender.userName;
                            ToastNotification.show("Tin nhắn từ " + senderName, finalMsg.content);

                            // Nếu vẫn đang mở trang Chat nhưng chat với người khác, cứ gửi msg vào UI (nếu cần xử lý thẻ bạn bè)
                            if (activeChatListener != null) {
                                activeChatListener.accept(finalMsg);
                            }
                        }
                    }
                    // Kịch bản 3: Mình là người gửi (đồng bộ tab) -> Chỉ đẩy vào UI nếu đang mở chat
                    else if (finalMsg.sender.userName.equals(SessionManager.userName) && activeChatListener != null) {
                        activeChatListener.accept(finalMsg);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ LỖI DỊCH JSON TOÀN CỤC");
        }
    }
}