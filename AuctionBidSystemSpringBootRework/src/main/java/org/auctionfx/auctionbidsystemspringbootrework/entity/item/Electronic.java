package org.auctionfx.auctionbidsystemspringbootrework.entity.item;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "electronics")
public class Electronic extends Item {
    private String brand;
    private int warrantyMonths;

    // GETTER VÀ SETTER


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
