package org.auctionfx.auctionbidsystemspringbootrework.dto.response;

import org.auctionfx.auctionbidsystemspringbootrework.enums.TransactionStatus;

import java.math.BigDecimal;

public class TransactionHistoryResponse {
    private String itemId; // THÊM DÒNG NÀY
    private String itemName;
    private BigDecimal amount;
    private TransactionStatus status;
    private String imageUrl; // BỔ SUNG TRƯỜNG NÀY

    public TransactionHistoryResponse(String itemId, String itemName, BigDecimal amount, TransactionStatus status, String imageUrl) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.amount = amount;
        this.status = status;
        this.imageUrl = imageUrl;
    }

    // GETTER VÀ SETTER


    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
