package org.auctionfx.auctionbidsystemspringbootrework.entity.user;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "sellers")
public class Seller extends Bidder {

    // KHAI BÁO THUỘC TÍNH
    private double rating = 0.0;

    // LẤY VÀ UPDATE THUỘC TÍNH
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
}

