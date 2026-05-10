package com.auction.model;

import java.util.List;

public class AuctionModel {
    public String id;
    public double highestBid;
    public String status;
    public String startTime;
    public String endTime;
    public ItemModel bidProduct;
    public SellerModel seller;

    // HỨNG DỮ LIỆU NGƯỜI THẮNG
    public BidderModel winningUser;

    public List<BidTransactionModel> bidTransactions;

    // ---- ĐÂY LÀ CLASS CHỨA THÔNG TIN SẢN PHẨM ----
    public static class ItemModel {
        public String id;
        public String name;
        public double startPrice;
        public String description;

        // THÊM DÒNG NÀY ĐỂ NHẬN DANH SÁCH LINK ẢNH TỪ SERVER
        public List<String> imageUrls;
    }

    public static class SellerModel {
        public String userName;
    }

    public static class BidderModel {
        public String userName;
    }

    public static class BidTransactionModel {
        public double bidAmount;
        public BidderModel bidder;
        public String bidTimestamp;
    }

    public double getMyHighestBid(String myUserName) {
        if (bidTransactions == null || bidTransactions.isEmpty() || myUserName == null) {
            return 0.0;
        }
        double maxBid = 0;
        for (BidTransactionModel tx : bidTransactions) {
            if (tx.bidder != null && myUserName.equals(tx.bidder.userName)) {
                if (tx.bidAmount > maxBid) {
                    maxBid = tx.bidAmount;
                }
            }
        }
        return maxBid;
    }
}