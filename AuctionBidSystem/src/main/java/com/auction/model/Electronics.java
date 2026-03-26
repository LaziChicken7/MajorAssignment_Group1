package com.auction.model;

import java.time.LocalDateTime;

public class Electronics extends Item{
    protected String brand;
    private int warrantyMonth;

    public Electronics(String id, String name, String information, double startPrice, double currentPrice, String sellerId, String status, LocalDateTime endTime, String brand, int warrantyMonth) {
        super(id, name, information, startPrice, currentPrice, sellerId, status, endTime);
        this.brand = brand;
        this.warrantyMonth = warrantyMonth;
    }

    public String brand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public int warrantyMonth() {
        return warrantyMonth;
    }

    public void setWarrantyMonth(int warrantyMonth) {
        this.warrantyMonth = warrantyMonth;
    }
}
