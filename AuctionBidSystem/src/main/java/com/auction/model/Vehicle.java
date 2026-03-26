package com.auction.model;

import java.time.LocalDateTime;

public class Vehicle extends Item{
    private String engineType;
    private double mileage;

    public Vehicle(String id, String name, String information, double startPrice, double currentPrice, String sellerId, String status, LocalDateTime endTime, String engineType) {
        super(id, name, information, startPrice, currentPrice, sellerId, status, endTime);
        this.engineType = engineType;
    }

    public String engineType() {
        return engineType;
    }

    public void setEngineType(String engineType) {
        this.engineType = engineType;
    }

    public double mileage() {
        return mileage;
    }

    public void setMileage(double mileage) {
        this.mileage = mileage;
    }
}
