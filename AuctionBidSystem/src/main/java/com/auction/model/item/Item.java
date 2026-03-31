package com.auction.model.item;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.auction.model.base.Entity;
import com.auction.model.user.Seller;

public class Item extends Entity{
    protected String name;
    protected String description;
    protected Seller seller;
    protected LocalDateTime startTime;
    protected BigDecimal startPrice;

    public Item(String id, String name, String description, BigDecimal startPrice) {
        super(id);
        this.name = name;
        this.description = description;
        this.startPrice = startPrice;
        this.seller = null;
        this.startTime = LocalDateTime.now();
    }

    // LẤY VÀ UPDATE THUỘC TÍNH
    public String getName() { return name; }
    public void updateName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void updateDescription(String description) { this.description = description; }

    public Seller getSeller() { return seller; }
    public void updateSeller(Seller seller) { this.seller = seller; }

    public LocalDateTime getStartTime() { return startTime; }
    public void updateStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public BigDecimal getStartPrice() { return startPrice; }
    public void updateStartPrice(BigDecimal startPrice) { this.startPrice = startPrice; }
}
