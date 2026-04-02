package com.auction.model.item;

import java.math.BigDecimal;

public class Electronics extends Item{
    private String brand;
    private int warrantyMonths;

    public Electronics(String id, String name, String description, BigDecimal startPrice, String brand, int warrantyMonths) {
        super(id, name, description, startPrice);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    // SET VÀ GET _ ĐỂ LẤY THUỘC TÍNH PRIVATE


    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }
    public int getWarrantyMonths() {
        return warrantyMonths;
    }
    public void setWarrantyMonths(int warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
    }
}
