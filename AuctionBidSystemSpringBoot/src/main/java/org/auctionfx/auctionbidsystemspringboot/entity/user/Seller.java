package org.auctionfx.auctionbidsystemspringboot.entity.user;

import org.auctionfx.auctionbidsystemspringboot.enums.Role;

public class Seller extends Bidder {

    // KHAI BÁO THUỘC TÍNH
    private double rating;

    public Seller(String userName, String password, String fullName, String email, String numberPhone, String citizenId, String address, Role role) {
        super(userName, password, fullName, email, numberPhone, citizenId, address, role);
        this.rating = 0;
    }

    // LẤY VÀ UPDATE THUỘC TÍNH
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    // LÀM TRÒN RATING 1 DẤU THẬP PHÂN
    // Cho phép quyền truy cập vào addItem

}
