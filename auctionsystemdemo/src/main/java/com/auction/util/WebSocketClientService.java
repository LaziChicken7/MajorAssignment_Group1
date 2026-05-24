package com.auction.util;

import javafx.application.Platform;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class WebSocketClientService {

    private WebSocketStompClient stompClient;
    private StompSession stompSession;

    // --- CÁC BIẾN PHỤC VỤ AUTO-RECONNECT ---
    private String savedUsername;
    private Consumer<String> savedCallback;
    private boolean intentionallyDisconnected = false; // Cờ đánh dấu người dùng chủ động đăng xuất
    private int reconnectAttempts = 0; // Đếm số lần thử lại
    private static final int RECONNECT_DELAY_MS = 5000; // Đợi 5 giây giữa các lần thử

    public void connect(String username, Consumer<String> onMessageReceived) {
        // Lưu lại thông tin để dùng cho việc kết nối lại
        this.savedUsername = username;
        this.savedCallback = onMessageReceived;
        this.intentionallyDisconnected = false;

        String wsUrl = ApiService.BASE_URL.replace("http", "ws") + "/ws-chat";

        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new StringMessageConverter());

        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                System.out.println("✅ STOMP: Đã kết nối thành công tới Server!");
                stompSession = session;
                reconnectAttempts = 0; // Kết nối thành công thì reset bộ đếm

                String destination = "/topic/messages/" + username;
                session.subscribe(destination, new StompFrameHandler() {

                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return String.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        if (payload != null) {
                            String json = (String) payload;
                            onMessageReceived.accept(json);
                        }
                    }
                });
            }

            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                System.err.println("❌ STOMP LỖI NỘI BỘ: " + exception.getMessage());
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                // ĐÃ XÓA ALERT LỖI UI. Thay vào đó là gọi hàm tự động kết nối lại ngầm
                System.err.println("⚠️ STOMP: Mất kết nối mạng hoặc Server sập!");
                scheduleReconnect();
            }
        };

        // TẠO HEADER ĐỂ BÁO DANH TÍNH CHO SERVER BIẾT AI ĐANG KẾT NỐI
        org.springframework.web.socket.WebSocketHttpHeaders handshakeHeaders = new org.springframework.web.socket.WebSocketHttpHeaders();
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("username", username); // Nhét tên user đang đăng nhập vào đây

        // NỐI KÈM THEO HEADER
        stompClient.connectAsync(wsUrl, handshakeHeaders, connectHeaders, sessionHandler);
    }

    /**
     * HÀM TỰ ĐỘNG KẾT NỐI LẠI (AUTO-RECONNECT)
     */
    private void scheduleReconnect() {
        // Nếu là do người dùng bấm Đăng Xuất (Log out) thì không kết nối lại nữa
        if (intentionallyDisconnected) {
            return;
        }

        reconnectAttempts++;
        System.out.println("🔄 STOMP: Đang thử kết nối lại... (Lần " + reconnectAttempts + ") sau 5 giây.");

        // Đẩy tiến trình chờ ra một luồng ngầm để KHÔNG LÀM ĐƠ JAVAFX UI
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(RECONNECT_DELAY_MS); // Nghỉ 5 giây
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Sau 5 giây, kiểm tra lại xem có bị chủ động ngắt không, nếu không thì gọi connect
            if (!intentionallyDisconnected) {
                connect(savedUsername, savedCallback);
            }
        });
    }

    public void sendMessage(String sender, String receiver, String content) {
        if (stompSession != null && stompSession.isConnected()) {

            Map<String, String> payloadMap = new HashMap<>();
            payloadMap.put("sender", sender);
            payloadMap.put("receiver", receiver);
            payloadMap.put("content", content);

            String jsonPayload = ApiService.gson.toJson(payloadMap);
            stompSession.send("/app/chat.send", jsonPayload);

        } else {
            System.err.println("❌ Không thể gửi tin: WebSocket chưa sẵn sàng!");
            // Nếu bạn muốn báo lỗi gửi tin thất bại cho user thì mở comment bên dưới,
            // Còn không thì bỏ qua để nó thử gửi lại sau khi reconnect
            // showErrorToUI("Lỗi Gửi Tin", "Mất kết nối tới Tổng đài chat. Tin nhắn chưa được gửi!");
        }
    }

    public void disconnect() {
        // Đánh dấu cờ này = TRUE để hệ thống Reconnect biết là "Chủ nhân muốn ngắt, đừng tự nối lại nữa"
        intentionallyDisconnected = true;

        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
        if (stompClient != null) {
            stompClient.stop();
        }
        System.out.println("🛑 STOMP: Đã ngắt kết nối hoàn toàn.");
    }

    // Đã xóa hàm showErrorToUI vì không cần dùng tới nữa
}