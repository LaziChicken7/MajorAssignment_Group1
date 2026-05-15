package com.auction.model;

public class ChatMessageModel {
    public String id;
    public ConnectionModel.UserModel sender;
    public ConnectionModel.UserModel receiver;
    public String content;
    public boolean isRead;
    public String sendTime;

    // Thuộc tính này dùng để tương thích với ChatMessageDTO bên Spring Boot
    public String timestamp;
}