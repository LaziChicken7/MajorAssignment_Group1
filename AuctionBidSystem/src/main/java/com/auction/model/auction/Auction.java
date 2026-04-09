package com.auction.model.auction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.auction.model.base.Entity;
import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;
import com.auction.model.user.UserManager;
import com.auction.model.enums.AuctionStatus;

class placeBidFailedException extends Exception {
    public placeBidFailedException(String msg) {
        super(msg);
    }
}

// Thêm class Antisnipping gia hạn thời gian

class AntiSnipping {
    private static final int SNIPING_THRESHOLD_SECONDS = 10; // X giây cuối
    private static final int EXTENSION_SECONDS = 60; // Gia hạn thêm Y giây

    // Gia hạn
    public static LocalDateTime handleAntiSniping(LocalDateTime endTime, LocalDateTime bidTimestamp) {
        LocalDateTime newEndTime = null;
        long secondsRemaining = ChronoUnit.SECONDS.between(bidTimestamp, endTime);
        if (secondsRemaining <= SNIPING_THRESHOLD_SECONDS) {
            newEndTime = endTime.plusSeconds(EXTENSION_SECONDS);
            System.err.println("Anti-snipping: Phiên đấu giá đã được gia hạn thêm " + EXTENSION_SECONDS + " giây.");
        }
        return newEndTime;
    }
}

// TOÀN BỘ CÁC HÀNH ĐỘNG ĐẤU GIÁ

// Chỉ đóng vai trò là một "Thực thể"
// Chỉ chứa thông tin (item, start/end time, currendBid) và logic của đúng MỘT phiên đấu giá (nhận đấu giá, kiểm tra hợp lệ)
public class Auction extends Entity {

    // KHAI BÁO ĐỐI TƯỢNG

    private final Item bidProduct;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;

    private BigDecimal highestBid;
    private Bidder winningUser;
    private final List<BidTransaction> bidTransactions;

    // KHAI BÁO CONSTRUCTOR
    public Auction(String id, Item bidProduct, Seller seller, LocalDateTime startTime, LocalDateTime endTime) {
        super(id);
        this.bidProduct = bidProduct;
        this.bidProduct.setSeller(seller);
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AuctionStatus.OPEN;
        this.highestBid = bidProduct.getStartPrice();
        this.winningUser = null;
        this.bidTransactions = new ArrayList<>();
    }


    // XỬ LÝ MỘT USER ĐẶT GIÁ
    public synchronized boolean placeBid(Bidder bidder, BigDecimal bidAmount, LocalDateTime bidTimestamp) throws NotEnoughMoneyException {
        // Kiểm tra tính hợp lệ cơ bản
        // 1. Kiểm tra xem phiên đấu giá có đang mở hay không
        if (status != AuctionStatus.RUNNING) {
            System.err.println("Lỗi: Phiên đấu giá không ở trạng thái mở để nhận đấu giá!");
            return false;
        }

        // 2. Cấm người bán tự đấu giá
        if (bidder.getId().equals(bidProduct.getSeller().getId())) {
            System.err.println("Lỗi: Người bán không thể đấu giá!");
            return false;
        }

        // 3. Kiểm tra xem phiên đấu giá hết thời gian chưa
        if (bidTimestamp.isAfter(endTime)) {
            System.err.println("Lỗi: Đã hết thời gian đấu giá!");
            return false;
        }

        if (bidAmount.compareTo(highestBid) <= 0) {
            System.err.println("Lỗi: Giá đấu giá không hợp lệ!");
            return false;
        }

        // 4. Hoàn lại tiền cho người đấu giá cũ
        if (winningUser != null) {
            try {
                PaymentService.unFreezeMoney(winningUser.getId(), highestBid);
            } catch (NotEnoughMoneyException e) {
                System.err.println(e.getMessage());
                throw new NotEnoughMoneyException(e.getMessage());
                // Chỉnh trong controller sau
            }
        }

        // 5. Cập nhật người dẫn đầu và giá cao nhất
        highestBid = bidAmount;
        winningUser = bidder;
        try {
            PaymentService.FreezeMoney(winningUser.getId(), bidAmount);
        } catch (NotEnoughMoneyException e) {
            System.err.println(e.getMessage());
            throw new NotEnoughMoneyException(e.getMessage());
            // Chỉnh trong controller sau
        }

        // 6. Lưu lịch sử giao dịch
        BidTransaction transaction = new BidTransaction(bidProduct, bidder, bidAmount, bidTimestamp);
        bidTransactions.add(transaction);

        // 7. Kích hoạt Anti-snipping Algorithm
        this.endTime = AntiSnipping.handleAntiSniping(this.endTime, bidTimestamp);

        return true;
    }


    // Chuyển trạng thái khi bắt đầu
    public void startAuction() {
        this.status = AuctionStatus.RUNNING;
    }

    // Chuyển trạng thái khi kết thúc và công bố người thắng
    public boolean closeAuction() {
        this.status = AuctionStatus.FINISHED;
        if (winningUser != null) {
            System.out.println("Phiên đấu giá kết thúc. Người thắng là: " + winningUser.getFullName());
            return true;
        } else {
            System.out.println("Phiên đấu giá kết thúc. Không có người thắng.");
            return false;
        }
        // Cập nhật trong thông báo xác nhận trả tiền hay không
    }

    // Chuyển sang trạng thái chấp nhận trả tiền
    public void acceptPayment() {
        this.status = AuctionStatus.PAID;
        try {
            PaymentService.transferMoney(winningUser.getId(), bidProduct.getSeller().getId(), highestBid);
        } catch (NotEnoughMoneyException e) {
            System.err.println(e.getMessage());
            // Chỉnh trong controller sau
        }
    }

    // Chuyển sang trạng thái hủy đấu giá
    public void cancelAuction() {
        this.status = AuctionStatus.CANCELLED;
        try {
            PaymentService.unFreezeMoney(winningUser.getId(), highestBid);
        } catch (NotEnoughMoneyException e) {
            System.err.println(e.getMessage());
            // Chỉnh trong controller sau
        }
    }

    // Các hàm get/ đối tượng
    public Item getBidProduct() { return bidProduct; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public AuctionStatus getStatus() { return status; }
    public BigDecimal getHighestBid() { return highestBid; }
    public Bidder getWinningUser() { return winningUser; }
    public List<BidTransaction> getBidTransactions() { return bidTransactions; }
}
