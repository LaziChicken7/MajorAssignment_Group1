package com.auction.model.user;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.auction.model.enums.Role;
import com.auction.model.item.Item;

// CẤP QUYỀN BIDDER
public class Bidder extends User {

    // KHAI BÁO THUỘC TÍNH
    protected static long bankingNumber = 1000000000L;
    protected String address;
    protected final String bankAccountNumber;
    protected BigDecimal moneyOnWallet;
    protected BigDecimal moneyinFrozen;

    protected List<Item> successBidItem;
    protected List<Item> failedBidItem;

    public Bidder(String userName, String password, String fullName, String email, String numberPhone, String citizenId, String address, Role role) {
        super(userName, password, fullName, email, numberPhone, citizenId, role);
        this.address = address;
        this.bankAccountNumber = String.valueOf(++bankingNumber);
        this.moneyOnWallet = BigDecimal.ZERO;
        this.moneyinFrozen = BigDecimal.ZERO;

        this.successBidItem = new ArrayList<>();
        this.failedBidItem = new ArrayList<>();
        this.role = role;
    }
    
    // LẤY THUỘC TÍNH
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getBankAccountNumber() { return bankAccountNumber; }

    public BigDecimal getMoneyOnWallet() { return moneyOnWallet; }
    public void setMoneyOnWallet(BigDecimal moneyOnWallet) { this.moneyOnWallet = moneyOnWallet; }

    public BigDecimal getMoneyinFrozen() { return moneyinFrozen; }
    public void setMoneyinFrozen(BigDecimal moneyinFrozen) { this.moneyinFrozen = moneyinFrozen; }

    // NẠP VÀ RÚT TIỀN
    public void Deposit(BigDecimal money) {
        this.moneyOnWallet = this.moneyOnWallet.add(money);
    }

    public void Withdraw(BigDecimal money) {
        this.moneyOnWallet = this.moneyOnWallet.subtract(money);
    }

    // CẬP NHẬT DANH SÁCH ĐẤU GIÁ
    public void addSuccessBidItem(Item item) {
        this.successBidItem.add(item);
    }

    public void addFailedBidItem(Item item) {
        this.failedBidItem.add(item);
    }
}
