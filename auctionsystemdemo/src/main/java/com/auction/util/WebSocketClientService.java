package com.auction.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class WebSocketClientService {

    private WebSocketStompClient stompClient;
    private StompSession stompSession;

    public void connect(String username, Consumer<String> onMessageReceived) {
        String wsUrl = ApiService.BASE_URL.replace("http", "ws") + "/ws-chat";

        stompClient = new WebSocketStompClient(new StandardWebSocketClient());

        // CHUẨN HOÁ 1: Dùng String Converter
        stompClient.setMessageConverter(new StringMessageConverter());

        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                System.out.println("✅ STOMP: Đã kết nối thành công tới Server!");
                stompSession = session;

                String destination = "/topic/messages/" + username;
                session.subscribe(destination, new StompFrameHandler() {

                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        // CHUẨN HOÁ 2: Bắt buộc yêu cầu String.class để khớp với StringMessageConverter
                        return String.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        if (payload != null) {
                            // Nhận chuỗi JSON gửi qua Controller
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
                showErrorToUI("Mất kết nối mạng", "Đường truyền WebSocket tới máy chủ đã bị ngắt!");
            }
        };

        stompClient.connectAsync(wsUrl, sessionHandler);
    }

    public void sendMessage(String sender, String receiver, String content) {
        if (stompSession != null && stompSession.isConnected()) {

            // CHUẨN HÓA 3: Đóng gói gửi bằng Map -> Gson (Không lỗi ký tự đặc biệt)
            Map<String, String> payloadMap = new HashMap<>();
            payloadMap.put("sender", sender);
            payloadMap.put("receiver", receiver);
            payloadMap.put("content", content);

            String jsonPayload = ApiService.gson.toJson(payloadMap);

            // Gửi dưới dạng String
            stompSession.send("/app/chat.send", jsonPayload);

        } else {
            showErrorToUI("Lỗi Gửi Tin", "Chưa kết nối được tới Tổng đài chat. Vui lòng thử lại!");
        }
    }

    public void disconnect() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
            stompClient.stop();
        }
    }

    private void showErrorToUI(String title, String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.show();
        });
    }
}