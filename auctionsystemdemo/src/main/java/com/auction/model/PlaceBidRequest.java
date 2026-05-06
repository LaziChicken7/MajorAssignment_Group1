package com.auction.model;

public class PlaceBidRequest {
    public String bidderUserName;
    public double bidAmount;

    public PlaceBidRequest(String bidderUserName, double bidAmount) {
        this.bidderUserName = bidderUserName;
        this.bidAmount = bidAmount;
    }

    public String getBidderUserName() {
        return bidderUserName;
    }

    public void setBidderUserName(String bidderUserName) {
        this.bidderUserName = bidderUserName;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(double bidAmount) {
        this.bidAmount = bidAmount;
    }
}