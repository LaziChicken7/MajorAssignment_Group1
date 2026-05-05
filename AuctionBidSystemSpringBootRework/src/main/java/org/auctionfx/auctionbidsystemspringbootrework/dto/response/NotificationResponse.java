package org.auctionfx.auctionbidsystemspringbootrework.dto.response;

import org.auctionfx.auctionbidsystemspringbootrework.enums.NotificationType;

import java.time.LocalDateTime;

public class NotificationResponse {
    private String notificationId;
    private String title;
    private String description;
    private NotificationType type;
    private boolean isRead;
    private LocalDateTime createdAt;

    public NotificationResponse(String notificationId, String title, String description, NotificationType type, boolean isRead, LocalDateTime createdAt) {
        this.notificationId = notificationId;
        this.title = title;
        this.description = description;
        this.type = type;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    // GETTER VÀ SETTER

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
