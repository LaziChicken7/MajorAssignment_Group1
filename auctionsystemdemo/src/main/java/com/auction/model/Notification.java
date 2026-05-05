package com.auction.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Notification implements Serializable {
    private String message;
    private String type; // "ACTION" (vừa làm gì đó), "SUCCESS" (thành công)
    private String timestamp;

    public Notification(String message, String type) {
        this.message = message;
        this.type = type;
        // Tự động lấy giờ hiện tại khi tạo thông báo
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM"));
    }

    public String getMessage() { return message + " [" + timestamp + "]"; }
    public String getType() { return type; }
}