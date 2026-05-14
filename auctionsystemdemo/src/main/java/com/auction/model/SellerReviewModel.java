package com.auction.model;

public class SellerReviewModel {
    public String id;
    public int star;
    public String comment;
    public String createdAt;
    public Reviewer reviewer;

    public static class Reviewer {
        public String userName;
        public String fullName;
        public String avatarUrl;
    }
}