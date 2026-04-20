package org.auctionfx.auctionbidsystemspringboot.entity.auction;

import org.auctionfx.auctionbidsystemspringboot.entity.base.BaseEntity;
import org.auctionfx.auctionbidsystemspringboot.entity.item.Item;
import org.auctionfx.auctionbidsystemspringboot.entity.user.Bidder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BidTransaction extends BaseEntity {

    // KHAI BÁO ĐỐI TƯỢNG
    private static int transactionCounter = 0;
    private final Item item;
    private final Bidder bidder;
    private final BigDecimal bidAmount;
    private final LocalDateTime bidTimestamp;

    public BidTransaction(Item item, Bidder bidder, BigDecimal bidAmount, LocalDateTime bidTimestamp) {
        super("BID" + (++transactionCounter));
        this.item = item;
        this.bidder = bidder;
        this.bidAmount = bidAmount;
        this.bidTimestamp = bidTimestamp;
    }

    // LẤY VÀ UPDATE THUỘC TÍNH
    public Item getItem() { return item; }
    public Bidder getBidder() { return bidder; }
    public BigDecimal getBidAmount() { return bidAmount; }
    public LocalDateTime getBidTimestamp() { return bidTimestamp; }
}
