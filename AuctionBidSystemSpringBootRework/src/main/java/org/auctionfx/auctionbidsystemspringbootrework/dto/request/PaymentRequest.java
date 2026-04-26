package org.auctionfx.auctionbidsystemspringbootrework.dto.request;

import java.math.BigDecimal;

public class PaymentRequest {
    private String userName;
    private BigDecimal amount;

    // GETTER VÀ SETTER

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
