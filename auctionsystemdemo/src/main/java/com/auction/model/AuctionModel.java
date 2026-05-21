package com.auction.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AuctionModel {
    public String id;
    public double highestBid;
    public String status;

    // [BỔ SUNG]: Hứng trạng thái thanh toán từ Server (Ví dụ: SUCCESS, FAILED, PAID...)
    public String transactionStatus;

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

        public String itemType;          // Phân loại: ART, ELECTRONIC, VEHICLE

        // 1. Dành cho mặt hàng Nghệ thuật (ART)
        @SerializedName(value = "nameAuthor", alternate = {"name_author"})
        public String nameAuthor;

        @SerializedName(value = "creationYear", alternate = {"creation_year"})
        public Integer creationYear;

        // 2. Dành cho đồ Điện tử (ELECTRONIC)
        public String brand;

        @SerializedName(value = "warrantyMonths", alternate = {"warranty_months"})
        public Integer warrantyMonths;

        // 3. Dành cho Phương tiện (VEHICLE)
        @SerializedName(value = "engineType", alternate = {"engine_type"})
        public String engineType;

        public Integer mileage;
    }

    public static class SellerModel {
        public String userName;
        public String fullName;
        public String avatarUrl;

        // [TỐI ƯU]: Chuyển sang Double (chữ D viết hoa) để an toàn nếu Server trả về null (chưa có ai đánh giá)
        public Double rating;
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