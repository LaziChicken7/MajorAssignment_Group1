package com.auction.model;

//CẤP QUYỀN BIDDER
public class Bidder extends User {
    //KHAI BÁO THUỘC TÍNH

    protected static long idCounter = 0;
    protected static long bankingNumber = 1000000000L;
    protected String address;
    protected final String bankAccountNumber;
    protected double moneyOnWallet;
    protected double moneyinFrozen;
    //Database History Transaction
    //Database Pending Product (Frozen Product)

    public Bidder(String userName, String password, String fullName, String email, String numberPhone, String citizenId, String address) {
        super(userName, password, fullName, email, numberPhone, citizenId);
        this.address = address;
        this.bankAccountNumber = String.valueOf(++bankingNumber);
        this.moneyOnWallet = 0;
        this.moneyinFrozen = 0;
    }
    
    //LẤY THUỘC TÍNH

    public String getAddress() { return address; }
    public void updateAddress(String address) { this.address = address; }

    public String getBankAccountNumber() { return bankAccountNumber; }

    public double getMoneyOnWallet() { return moneyOnWallet; }
    public void updateMoneyOnWallet(double moneyOnWallet) { this.moneyOnWallet = moneyOnWallet; }

    public double getMoneyinFrozen() { return moneyinFrozen; }
    public void updateMoneyinFrozen(double moneyinFrozen) { this.moneyinFrozen = moneyinFrozen; }

    //NẠP VÀ RÚT TIỀN

    public void Deposit(double money) {
        this.moneyOnWallet += money;
    }

    public void Withdraw(double money) {
        this.moneyOnWallet -= money;
    }

    //ĐÓNG BĂNG VÀ HỦY ĐÓNG BĂNG MẶT HÀNG

    public void Freeze(double moneyBidding) {
        this.moneyOnWallet -= moneyBidding;
        this.moneyinFrozen += moneyBidding;
    }

    public void Unfreeze(double moneyBidding) {
        this.moneyOnWallet += moneyBidding;
        this.moneyinFrozen -= moneyBidding;
    }

}
