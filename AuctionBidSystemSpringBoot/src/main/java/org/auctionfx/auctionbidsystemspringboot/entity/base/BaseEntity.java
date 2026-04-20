package org.auctionfx.auctionbidsystemspringboot.entity.base;

public abstract class BaseEntity {
    protected String id;

    public BaseEntity(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
