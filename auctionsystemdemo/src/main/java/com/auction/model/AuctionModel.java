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

        // DANH SÁCH LINK ẢNH TỪ SERVER
        public List<String> imageUrls;

        // --- BỔ SUNG CÁC TRƯỜNG NÀY ĐỂ HỨNG THÔNG TIN KỸ THUẬT TỪ BACKEND ---
        public String itemType;          // Phân loại: ART, ELECTRONIC, VEHICLE

        // Dành cho mặt hàng Nghệ thuật (ART)
        public String nameAuthor;
        public Integer creationYear;     // Dùng Integer để tránh lỗi nếu null

        // Dành cho đồ Điện tử (ELECTRONIC)
        public String brand;
        public Integer warrantyMonths;

        // Dành cho Phương tiện (VEHICLE)
        public String engineType;
        public Integer mileage;
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