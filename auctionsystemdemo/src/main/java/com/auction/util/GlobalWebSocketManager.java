package com.auction.util;

import com.auction.model.ChatMessageModel;
import javafx.application.Platform;

import java.util.function.Consumer;

public class GlobalWebSocketManager {

    private static WebSocketClientService webSocketService;
    private static Consumer<ChatMessageModel> activeChatListener = null;
    public static String currentActiveChatPartner = null;

    // KHO CHỨA DANH SÁCH CHẤM XANH TOÀN CỤC
    public static final java.util.Set<String> globalUnreadUsers = java.util.concurrent.ConcurrentHashMap.newKeySet();

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
                    // KỊCH BẢN 1: MÀN HÌNH CHAT ĐANG MỞ
                    if (activeChatListener != null) {
                        try {
                            activeChatListener.accept(finalMsg);
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                    // KỊCH BẢN 2: MÀN HÌNH CHAT ĐANG ĐÓNG (Ở Trang chủ, Ví...)
                    else {
                        if (finalMsg.receiver.userName.equalsIgnoreCase(SessionManager.userName)) {

                            // 1. Thêm người gửi vào kho chấm xanh
                            globalUnreadUsers.add(finalMsg.sender.userName.toLowerCase());

                            // 2. GỌI MAIN CONTROLLER ĐỂ CẬP NHẬT CÁI SỐ ĐỎ BÊN THANH SIDEBAR
                            if (com.auction.controller.dashboard.MainController.getInstance() != null) {
                                com.auction.controller.dashboard.MainController.getInstance().updateChatBadgeCount();
                            }

                            // 3. Hiện Toast
                            try {
                                String senderName = finalMsg.sender.fullName != null ? finalMsg.sender.fullName : finalMsg.sender.userName;
                                ToastNotification.show("Tin nhắn từ " + senderName, finalMsg.content, ToastNotification.ToastType.CHAT, () -> {
                                    if (com.auction.controller.dashboard.MainController.getInstance() != null) {
                                        com.auction.controller.dashboard.MainController.getInstance().openSpecificChat(finalMsg.sender.userName);
                                    }
                                });
                            } catch (Exception e) { e.printStackTrace(); }
                        }
                    }
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void disconnect() {
        if (webSocketService != null) {
            webSocketService.disconnect();
            webSocketService = null;
        }
        currentActiveChatPartner = null;
        activeChatListener = null;
        globalUnreadUsers.clear();
    }

    // ==============================================================
    // GỌI HÀM NGHE ĐẤU GIÁ TỪ WEBSOCKET SERVICE
    // ==============================================================
    public static void listenToAuction(String auctionId, Runnable onNewBid) {
        if (webSocketService != null) {
            webSocketService.subscribeToAuction(auctionId, onNewBid);
        }
    }

    public static void stopListeningAuction() {
        if (webSocketService != null) {
            webSocketService.unsubscribeAuction();
        }
    }

    // ==============================================================
    // GỌI HÀM NGHE TOÀN CẦU
    // ==============================================================
    public static void listenToGlobalAuctions(Runnable onUpdate) {
        if (webSocketService != null) {
            webSocketService.subscribeToGlobalAuctions(onUpdate);
        }
    }
    public static void stopListeningGlobalAuctions() {
        if (webSocketService != null) {
            webSocketService.unsubscribeGlobalAuctions();
        }
    }
}