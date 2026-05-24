package com.auction.model;

public class ConnectionModel {
    public String id;
    public UserModel sender;
    public UserModel receiver;
    public String status;
    public String createdAt;

    // Lớp phụ để lấy thông tin user cơ bản
    public static class UserModel {
        public String userName;
        public String fullName;
        public String avatarUrl;

        public String role; // <-- BẠN THÊM DÒNG NÀY VÀO LÀ HẾT BÁO LỖI
    }
}