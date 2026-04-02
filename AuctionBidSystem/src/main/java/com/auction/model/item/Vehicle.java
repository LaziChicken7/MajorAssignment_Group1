package com.auction.model.item;

import java.math.BigDecimal;

public class Vehicle extends Item{
    private String engineType;
    private int mileage;

    public Vehicle(String id, String name, String description, BigDecimal startPrice, String engineType, int mileage) {
        super(id, name, description, startPrice);
        this.engineType = engineType;
        this.mileage = mileage;
    }

    //  SET VÀ GET _ ĐỂ LẤY THUỘC TÍNH

    public String getEngineType() {
        return engineType;
    }
    public void setEngineType(String engineType) {
        this.engineType = engineType;
    }
    public int getMileage() {
        return mileage;
    }
    public void setMileage(int mileage) {
        this.mileage = mileage;
    }
}
