package com.auction.model;

public class AuctionItem {
    private String id;
    private String name;
    private double currentPrice;
    private String endTime;
    private String status; // OPEN, RUNNING, FINISHED

    public AuctionItem(String id, String name, double currentPrice, String endTime, String status) {
        this.id = id;
        this.name = name;
        this.currentPrice = currentPrice;
        this.endTime = endTime;
        this.status = status;
    }

    // Bắt buộc phải có các Getter này để JavaFX TableView có thể đọc được dữ liệu
    public String getId() { return id; }
    public String getName() { return name; }
    public double getCurrentPrice() { return currentPrice; }
    public String getEndTime() { return endTime; }
    public String getStatus() { return status; }
}