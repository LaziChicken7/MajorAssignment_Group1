package com.auction.model;

public class Bidder extends User{
    private double balance;
    private String address;

    public Bidder(String id, String username, String password, String fullname, String email, String numberPhone, String cccd, double balance, String address) {
        super(id, username, password, fullname, email, numberPhone, cccd);
        this.balance = balance;
        this.address = address;
    }

    public double balance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public String address() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
