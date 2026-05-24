package org.auctionfx.auctionbidsystemspringbootrework.entity.auction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.auctionfx.auctionbidsystemspringbootrework.entity.base.BaseEntity;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Bidder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "auto_bid_configs")
public class AutoBidConfig extends BaseEntity {
    // Nối ngược lại với bảng Auction
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    @JsonIgnore
    private Auction auction;

    // Nối với người dùng thiết lập
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidder_id", nullable = false)
    private Bidder bidder;

    @Column(nullable = false)
    private BigDecimal maxBidAmount; // Mức giá kịch trần sẵn sàng trả

    @Column(nullable = false)
    private boolean isActive = true; // Trạng thái: True là Bot đang chạy, False là Bot đã tắt (do hết tiền max)

    private LocalDateTime createdAt = LocalDateTime.now(); // Ưu tiên người cài đặt bot trước nếu bằng giá nhau

    // GETTER VÀ SETTER

    public Auction getAuction() {
        return auction;
    }

    public void setAuction(Auction auction) {
        this.auction = auction;
    }

    public Bidder getBidder() {
        return bidder;
    }

    public void setBidder(Bidder bidder) {
        this.bidder = bidder;
    }

    public BigDecimal getMaxBidAmount() {
        return maxBidAmount;
    }

    public void setMaxBidAmount(BigDecimal maxBidAmount) {
        this.maxBidAmount = maxBidAmount;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
