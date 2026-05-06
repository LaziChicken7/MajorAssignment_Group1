package com.auction.model;

public class AuctionCreationRequest {
    public String itemId;
    public String startTime;
    public String endTime;

    public AuctionCreationRequest(String itemId, String startTime, String endTime) {
        this.itemId = itemId;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}