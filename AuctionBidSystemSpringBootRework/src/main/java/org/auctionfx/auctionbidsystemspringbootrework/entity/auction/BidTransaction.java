package org.auctionfx.auctionbidsystemspringbootrework.entity.auction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.auctionfx.auctionbidsystemspringbootrework.entity.base.BaseEntity;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Bidder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bid_transactions")
public class BidTransaction extends BaseEntity {

    private BigDecimal bidAmount;
    private LocalDateTime bidTimestamp;

    @ManyToOne
    @JoinColumn(name = "bidder_id")
    private Bidder bidder;

    @ManyToOne
    @JoinColumn(name = "auction_id")
    @JsonIgnore // Quan trọng: Tránh lỗi lặp vô tận khi in ra JSON
    private Auction auction;

    // GETTER VÀ SETTER

    public BigDecimal getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(BigDecimal bidAmount) {
        this.bidAmount = bidAmount;
    }

    public LocalDateTime getBidTimestamp() {
        return bidTimestamp;
    }

    public void setBidTimestamp(LocalDateTime bidTimestamp) {
        this.bidTimestamp = bidTimestamp;
    }

    public Bidder getBidder() {
        return bidder;
    }

    public void setBidder(Bidder bidder) {
        this.bidder = bidder;
    }

    public Auction getAuction() {
        return auction;
    }

    public void setAuction(Auction auction) {
        this.auction = auction;
    }
}
