package com.auction.model;

public class Seller extends User {
    private double rating;
    private String bankAccountNumber;

    public Seller(String id, String username, String password, String fullname, String email, String numberPhone, String cccd, double rating, String bankAccountNumber) {
        super(id, username, password, fullname, email, numberPhone, cccd);
        this.rating = rating;
        this.bankAccountNumber = bankAccountNumber;
    }

    public double rating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String bankAccountNumber() {
        return bankAccountNumber;
    }

    public void setBankAccountNumber(String bankAccountNumber) {
        this.bankAccountNumber = bankAccountNumber;
    }
}
