package org.auctionfx.auctionbidsystemspringbootrework.dto.request;

import java.math.BigDecimal;

public class PlaceBidRequest {
    private String bidderUserName;
    private BigDecimal bidAmount;

    // GETTER VÀ SETTER

    public String getBidderUserName() {
        return bidderUserName;
    }

    public void setBidderUserName(String bidderUserName) {
        this.bidderUserName = bidderUserName;
    }

    public BigDecimal getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(BigDecimal bidAmount) {
        this.bidAmount = bidAmount;
    }
}
