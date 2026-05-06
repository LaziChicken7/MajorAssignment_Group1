package com.auction.model;

import java.util.List;

public class AuctionModel {
    public String id;
    public double highestBid;
    public String status;
    public String endTime;
    public ItemModel bidProduct;
    public SellerModel seller;

    // Thêm danh sách lịch sử đấu giá
    public List<BidTransactionModel> bidTransactions;

    public static class ItemModel {
        public String id;
        public String name;
        public double startPrice;
        public String description;
    }

    public static class SellerModel {
        public String userName;
    }

    // Class đại diện cho người đặt giá
    public static class BidderModel {
        public String userName;
    }

    // Class đại diện cho 1 lượt đặt giá
    public static class BidTransactionModel {
        public double bidAmount;
        public BidderModel bidder;
    }

    // HÀM TIỆN ÍCH: Tự tìm trong lịch sử xem giá cao nhất của "tôi" là bao nhiêu
    public double getMyHighestBid(String myUserName) {
        if (bidTransactions == null || bidTransactions.isEmpty() || myUserName == null) {
            return 0.0; // Chưa từng đấu giá
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