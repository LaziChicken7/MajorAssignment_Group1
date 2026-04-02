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
    protected BigDecimal endPrice;

    public Item(String id, String name, String description, BigDecimal startPrice) {
        super(id);
        this.name = name;
        this.description = description;
        this.startPrice = startPrice;
        this.seller = null;
        this.startTime = LocalDateTime.now();
    }

    // SET VÀ GET _ LẤY THUỘC TÍNH

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Seller getSeller() { return seller; }
    public void setSeller(Seller seller) { this.seller = seller; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public BigDecimal getStartPrice() { return startPrice; }
    public BigDecimal getEndPrice() {return endPrice; }
    public void setEndPrice(BigDecimal endPrice) { this.endPrice = endPrice; }

}
