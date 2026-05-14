package org.auctionfx.auctionbidsystemspringbootrework.dto.request;

public class ReviewRequest {
    private String reviewerUsername;
    private String sellerUsername;
    private int star;
    private String comment;

    // CONSTRUCTOR

    public ReviewRequest() {
    }

    // GETTER VÀ SETTER

    public String getReviewerUsername() {
        return reviewerUsername;
    }

    public void setReviewerUsername(String reviewerUsername) {
        this.reviewerUsername = reviewerUsername;
    }

    public String getSellerUsername() {
        return sellerUsername;
    }

    public void setSellerUsername(String sellerUsername) {
        this.sellerUsername = sellerUsername;
    }

    public int getStar() {
        return star;
    }

    public void setStar(int star) {
        this.star = star;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
