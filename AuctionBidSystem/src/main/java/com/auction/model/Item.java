package com.auction.model;
import java.time.LocalDateTime;

public class Item extends Entity{
    protected String name;
    protected String information;
    protected double startPrice;
    protected double currentPrice;
    protected String sellerId;
    protected String status;            // running _ finished _ closed
    protected LocalDateTime endTime;

    public Item(String id, String name, String information, double startPrice, double currentPrice, String sellerId, String status, LocalDateTime endTime) {
        super(id);
        this.name = name;
        this.information = information;
        this.startPrice = startPrice;
        this.currentPrice = currentPrice;
        this.sellerId = sellerId;
        this.status = status;

        if (endTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Thời gian kết thúc không hợp lệ");
        }

        this.endTime = endTime;
    }
}
