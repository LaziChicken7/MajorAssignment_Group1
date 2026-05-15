package org.auctionfx.auctionbidsystemspringbootrework.dto.request;

public class ChatMessageDTO {
    private String sender;    // Username người gửi
    private String receiver;  // Username người nhận
    private String content;   // Nội dung tin nhắn
    private String timestamp; // Giờ gửi

    // GETTER VÀ SETTER

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
