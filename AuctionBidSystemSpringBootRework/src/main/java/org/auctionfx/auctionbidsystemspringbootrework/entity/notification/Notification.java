package org.auctionfx.auctionbidsystemspringbootrework.entity.notification;

import jakarta.persistence.*;
import org.auctionfx.auctionbidsystemspringbootrework.entity.auction.Auction;
import org.auctionfx.auctionbidsystemspringbootrework.entity.base.BaseEntity;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.User;
import org.auctionfx.auctionbidsystemspringbootrework.enums.NotificationType;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // Thông báo này gửi cho ai?

    @ManyToOne
    @JoinColumn(name = "auction_id")
    private Auction auction; // Liên kết đến phiên đấu giá nào?

    private String title;
    private String description;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private boolean isRead = false; // Đã đọc chưa (Để thể hiện số thông báo màu đỏ góc dưới)
    private LocalDateTime createdAt = LocalDateTime.now();

    // GETTER VÀ SETTER

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Auction getAuction() {
        return auction;
    }

    public void setAuction(Auction auction) {
        this.auction = auction;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
