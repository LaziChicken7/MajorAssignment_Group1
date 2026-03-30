package com.auction.model.user;

public class Seller extends Bidder {
    // KHAI BÁO THUỘC TÍNH
    private double rating;

    public Seller(String userName, String password, String fullName, String email, String numberPhone, String citizenId, String address) {
        super(userName, password, fullName, email, numberPhone, citizenId, address);
        this.rating = 0;
    }

    // LẤY VÀ UPDATE THUỘC TÍNH
    public double rating() { return rating; }
    public void updateRating(double rating) { this.rating = rating; }

    // Cho phép quyền truy cập vào addItem
}
