package com.auction.model;

public class ReviewRequest {
    public String reviewerUsername;
    public String sellerUsername;
    public int star;
    public String comment;

    public ReviewRequest(String reviewerUsername, String sellerUsername, int star, String comment) {
        this.reviewerUsername = reviewerUsername;
        this.sellerUsername = sellerUsername;
        this.star = star;
        this.comment = comment;
    }
}