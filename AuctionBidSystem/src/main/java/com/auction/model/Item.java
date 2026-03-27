package com.auction.model;
import java.time.LocalDateTime;

public class Item extends Entity{
    protected String name;
    protected String description;
    protected Seller seller;
    protected LocalDateTime startTime;
    protected double startPrice;

    public Item(String id, String name, String description, double startPrice) {
        super(id);
        this.name = name;
        this.description = description;
        this.startPrice = startPrice;
        this.seller = null;
        this.startTime = LocalDateTime.now();
    }
}
