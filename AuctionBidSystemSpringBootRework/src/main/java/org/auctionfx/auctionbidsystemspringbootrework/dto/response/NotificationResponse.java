package org.auctionfx.auctionbidsystemspringbootrework.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    // https://currencylayer.com/documentation
    // Api trả về User sẽ loại bỏ kết quả là Null
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApiResponse<T> {
        // Cần một code để User có thể biết code để lookup xem code lỗi đó như nào
        private int code = 1000;
        // Thông báo api trả về cho User
        private String message;
        // Kết quả trả về
        private T result;

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public T getResult() {
            return result;
        }

        public void setResult(T result) {
            this.result = result;
        }
    }
}
