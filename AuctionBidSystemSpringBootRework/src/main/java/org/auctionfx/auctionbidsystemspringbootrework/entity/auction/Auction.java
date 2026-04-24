package org.auctionfx.auctionbidsystemspringbootrework.entity.auction;

import jakarta.persistence.*;
import org.auctionfx.auctionbidsystemspringbootrework.entity.base.BaseEntity;
import org.auctionfx.auctionbidsystemspringbootrework.entity.item.Item;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Bidder;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Seller;
import org.auctionfx.auctionbidsystemspringbootrework.enums.AuctionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "auctions")
public class Auction extends BaseEntity {

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal highestBid;

    @ManyToOne
    @JoinColumn(name = "winning_user_id")
    private Bidder winningUser;

    @Enumerated(EnumType.STRING)
    private AuctionStatus status = AuctionStatus.OPEN;

    // MQH một một với khóa ngoài item_id
    @OneToOne
    @JoinColumn(name = "item_id")
    private Item bidProduct;

    // MQH Nhiều một (Nhiều Auction với một Seller) với khóa ngoài seller_id
    @ManyToOne
    @JoinColumn(name = "seller_id")
    private Seller seller;

    // MQH Một nhiều (Một Auction với nhiều lượt trả giá)
    // Nếu Auction bị xóa, toàn bộ Bid cũng bị xóa theo
    @OneToMany(mappedBy = "auction", cascade = CascadeType.ALL)
    private List<BidTransaction> bidTransactions = new ArrayList<>();

    // GETTER VÀ SETTER

    public Bidder getWinningUser() {
        return winningUser;
    }

    public void setWinningUser(Bidder winningUser) {
        this.winningUser = winningUser;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public BigDecimal getHighestBid() {
        return highestBid;
    }

    public void setHighestBid(BigDecimal highestBid) {
        this.highestBid = highestBid;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public Item getBidProduct() {
        return bidProduct;
    }

    public void setBidProduct(Item bidProduct) {
        this.bidProduct = bidProduct;
    }

    public Seller getSeller() {
        return seller;
    }

    public void setSeller(Seller seller) {
        this.seller = seller;
    }

    public List<BidTransaction> getBidTransactions() {
        return bidTransactions;
    }

    public void setBidTransactions(List<BidTransaction> bidTransactions) {
        this.bidTransactions = bidTransactions;
    }
}
