package org.auctionfx.auctionbidsystemspringbootrework.entity.user;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "bidders")
public class Bidder extends User {

    // KHAI BÁO THUỘC TÍNH
    protected String address;
    protected String bankAccountNumber;
    protected BigDecimal moneyOnWallet = BigDecimal.ZERO;
    protected BigDecimal moneyinFrozen = BigDecimal.ZERO;

    // LẤY THUỘC TÍNH

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBankAccountNumber() {
        return bankAccountNumber;
    }

    public void setBankAccountNumber(String bankAccountNumber) {
        this.bankAccountNumber = bankAccountNumber;
    }

    public BigDecimal getMoneyOnWallet() {
        return moneyOnWallet;
    }

    public void setMoneyOnWallet(BigDecimal moneyOnWallet) {
        this.moneyOnWallet = moneyOnWallet;
    }

    public BigDecimal getMoneyinFrozen() {
        return moneyinFrozen;
    }

    public void setMoneyinFrozen(BigDecimal moneyinFrozen) {
        this.moneyinFrozen = moneyinFrozen;
    }
}
