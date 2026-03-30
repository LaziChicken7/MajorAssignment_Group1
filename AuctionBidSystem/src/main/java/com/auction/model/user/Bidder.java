package com.auction.model.user;

import java.util.ArrayList;
import java.util.List;

// CẤP QUYỀN BIDDER
public class Bidder extends User {
    // KHAI BÁO THUỘC TÍNH

    protected static long idCounter = 0;
    protected static long bankingNumber = 1000000000L;
    protected String address;
    protected final String bankAccountNumber;
    protected double moneyOnWallet;
    protected double moneyinFrozen;

    protected List<Item> successBidItem;
    protected List<Item> failedBidItem;

    public Bidder(String userName, String password, String fullName, String email, String numberPhone, String citizenId, String address) {
        super(userName, password, fullName, email, numberPhone, citizenId);
        this.address = address;
        this.bankAccountNumber = String.valueOf(++bankingNumber);
        this.moneyOnWallet = 0;
        this.moneyinFrozen = 0;

        this.successBidItem = new ArrayList<>();
        this.failedBidItem = new ArrayList<>();
    }
    
    // LẤY THUỘC TÍNH
    public String getAddress() { return address; }
    public void updateAddress(String address) { this.address = address; }

    public String getBankAccountNumber() { return bankAccountNumber; }

    public double getMoneyOnWallet() { return moneyOnWallet; }
    public void updateMoneyOnWallet(double moneyOnWallet) { this.moneyOnWallet = moneyOnWallet; }

    public double getMoneyinFrozen() { return moneyinFrozen; }
    public void updateMoneyinFrozen(double moneyinFrozen) { this.moneyinFrozen = moneyinFrozen; }

    // NẠP VÀ RÚT TIỀN
    public void Deposit(double money) {
        this.moneyOnWallet += money;
    }

    public void Withdraw(double money) {
        this.moneyOnWallet -= money;
    }
}
