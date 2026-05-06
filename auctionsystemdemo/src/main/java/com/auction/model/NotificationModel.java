package com.auction.model;

public class NotificationModel {
    public String notificationId;
    public String title;
    public String description;
    public String type; // PAYMENT_VERIFICATION, AUCTION_SUCCESS, AUCTION_FAILED
    public boolean isRead;
    public String createdAt;
}