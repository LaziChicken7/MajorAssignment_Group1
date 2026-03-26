package com.auction.model;

import java.time.LocalDateTime;

public class Art extends Item{
    private String artistName;
    private int creationYear;

    public Art(String id, String name, String information, double startPrice, double currentPrice, String sellerId, String status, LocalDateTime endTime, String artistName, int creationYear) {
        super(id, name, information, startPrice, currentPrice, sellerId, status, endTime);
        this.artistName = artistName;
        this.creationYear = creationYear;
    }

    public String artistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public int creationYear() {
        return creationYear;
    }

    public void setCreationYear(int creationYear) {
        this.creationYear = creationYear;
    }
}
