package com.auction.model.item;

import com.auction.model.enums.ItemType;

import java.math.BigDecimal;

public class ItemFactory {
    public static Item createItem(ItemType type, String id, String name, String description, BigDecimal startPrice){
        switch (type) {
            case ART:
                return new Art(id, name, description, startPrice, "unknown nameAuthor", 0);
            case VEHICLE:
                return new Vehicle(id, name, description, startPrice,"unknown engine",0);
            case ELECTRONICS:
                return new Electronics(id, name, description, startPrice,"unknown brand", 0);
            default:
                throw new IllegalArgumentException("Type Error");
        }
    }
}
