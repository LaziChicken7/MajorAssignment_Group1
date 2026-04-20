package org.auctionfx.auctionbidsystemspringboot.entity.item;



import org.auctionfx.auctionbidsystemspringboot.enums.ItemType;

import java.math.BigDecimal;

public class ItemFactory {
    public static Item createItem(ItemType type, String id, String name, String description, BigDecimal startPrice){
        return switch (type) {
            case ART -> new Art(id, name, description, startPrice, "unknown nameAuthor", 0);
            case VEHICLE -> new Vehicle(id, name, description, startPrice, "unknown engine", 0);
            case ELECTRONICS -> new Electronics(id, name, description, startPrice, "unknown brand", 0);
            default -> throw new IllegalArgumentException("Type Error");
        };
    }
}
