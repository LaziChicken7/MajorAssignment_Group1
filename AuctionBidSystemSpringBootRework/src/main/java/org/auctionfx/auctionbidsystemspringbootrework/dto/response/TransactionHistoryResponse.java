package org.auctionfx.auctionbidsystemspringbootrework.dto.response;

import org.auctionfx.auctionbidsystemspringbootrework.enums.TransactionStatus;

import java.math.BigDecimal;

public class TransactionHistoryResponse {
    private String itemName;
    private BigDecimal amount;
    private TransactionStatus status;

    public TransactionHistoryResponse(String itemName, BigDecimal amount, TransactionStatus status) {
        this.itemName = itemName;
        this.amount = amount;
        this.status = status;
    }

    // GETTER VÀ SETTER

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
}
