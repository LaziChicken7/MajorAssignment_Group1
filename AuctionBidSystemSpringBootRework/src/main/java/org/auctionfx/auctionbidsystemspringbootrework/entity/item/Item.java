package org.auctionfx.auctionbidsystemspringbootrework.entity.item;

import jakarta.persistence.*;
import org.auctionfx.auctionbidsystemspringbootrework.entity.base.BaseEntity;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Seller;
import org.auctionfx.auctionbidsystemspringbootrework.enums.ItemType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "items")
@Inheritance(strategy = InheritanceType.JOINED)
public class Item extends BaseEntity {
    // KHAI BÁO THUỘC TÍNH
    private String name;
    private String description;

    private LocalDateTime startTime = LocalDateTime.now();
    private BigDecimal startPrice;
    private BigDecimal endPrice;

    @ManyToOne // MQH nhiều - một (nhiều item - một user)
    @JoinColumn(name = "seller_id") // Kết nối hai bảng thông qua khóa ngoài seller_id
    private Seller seller;

    @Enumerated(EnumType.STRING) // Lưu chữ SELLER, BIDDER
    protected ItemType itemType;

    
    // sinh ra một bảng phụ lưu URL ảnh, liên kết Item qua item_id
    @ElementCollection
    private List<String> imageUrls = new ArrayList<>();

    // GETTER VÀ SETTER

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public BigDecimal getStartPrice() {
        return startPrice;
    }

    public void setStartPrice(BigDecimal startPrice) {
        this.startPrice = startPrice;
    }

    public BigDecimal getEndPrice() {
        return endPrice;
    }

    public void setEndPrice(BigDecimal endPrice) {
        this.endPrice = endPrice;
    }

    public Seller getSeller() {
        return seller;
    }

    public void setSeller(Seller seller) {
        this.seller = seller;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public void setItemType(ItemType itemType) {
        this.itemType = itemType;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
}
