package com.auction.model.base;


public abstract class Entity {
    protected String id;

    public Entity(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
