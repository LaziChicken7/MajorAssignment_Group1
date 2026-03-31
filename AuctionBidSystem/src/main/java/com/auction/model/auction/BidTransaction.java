package com.auction.model.auction;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.auction.model.base.Entity;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;

public class BidTransaction extends Entity {

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
