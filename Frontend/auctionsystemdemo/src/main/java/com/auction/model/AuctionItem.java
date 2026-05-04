package com.auction.model;

import java.io.Serializable;

public class AuctionItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id, name, timeLeft, status, description;
    private double currentPrice, startPrice, myBid;
    private boolean isMine;

    // Constructor chuẩn với 7 tham số
    public AuctionItem(String id, String name, double startPrice, double currentPrice, String timeLeft, String status, String description, boolean isMine) {
        this.id = id;
        this.name = name;
        this.startPrice = startPrice;
        this.currentPrice = currentPrice;
        this.timeLeft = timeLeft;
        this.status = status;
        this.description = description;
        this.myBid = 0;
        this.isMine = isMine;
    }

    // --- CÁC HÀM GETTER ĐỂ HẾT LỖI ĐỎ ---
    public String getId() { return id; }
    public String getName() { return name; }
    public double getCurrentPrice() { return currentPrice; }
    public double getStartPrice() { return startPrice; }
    public double getMyBid() { return myBid; }
    public String getStatus() { return status; }
    public String getDescription() { return description; }
    public boolean isMine() { return isMine; }

    // Đã sửa từ getEndTime thành getTimeLeft để khớp với Controller của bạn
    public String getTimeLeft() { return timeLeft; }

    public String getStatusColor() {
        if (status == null) return "#e67e22";
        return switch (status.toUpperCase()) {
            case "FINISHED", "SUCCESS" -> "#2ecc71";
            case "CLOSED" -> "#e74c3c";
            default -> "#e67e22";
        };
    }


    // Setters
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
    public void setMyBid(double myBid) { this.myBid = myBid; }
}