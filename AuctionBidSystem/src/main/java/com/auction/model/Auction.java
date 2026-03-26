package com.auction.model;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

// TOÀN BỘ CÁC HÀNH ĐỘNG ĐẤU GIÁ


public class Auction extends Entity {

    // KHAI BÁO ĐỐI TƯỢNG
    public enum Status {
        OPEN, RUNNING, FINISHED, PAID, CANCELLED
    }

    private static int auctionCounter = 0;
    private final Item bidProduct;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;
    private Status status;

    // QUẢN LÝ NGƯỜI DẪN ĐẦU VÀ LỊCH SỬ (THAY THẾ BẰNG DATABASE TRONG TƯƠNG LAI)
    private double highestBid;
    private Bidder winningUser;
    private List<BidTransaction> bidTransactions; // Thay thế thành database trong tương lai

    // CẤU HÌNH CHO ANTI-SNIPING
    private static final int SNIPING_THRESHOLD_SECONDS = 10; // X giây cuối
    private static final int EXTENSION_SECONDS = 60; // Gia hạn Y giây

    // KHAI BÁO CONSTRUCTOR
    public Auction(Item bidProduct, LocalDateTime startTime, LocalDateTime endTime) {
        super("AUC" + (++auctionCounter));
        this.bidProduct = bidProduct;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = Status.OPEN;
        this.highestBid = bidProduct.startPrice;
        this.winningUser = null;
        this.bidTransactions = new ArrayList<>();
    }

    
    // XỬ LÝ MỘT USER ĐẶT GIÁ
    public synchronized boolean placeBid(Bidder bidder, double bidAmount, LocalDateTime bidTimestamp) {
        // Kiểm tra tính hợp lệ cơ bản
        // 1. Kiểm tra xem phiên đấu giá có đang mở hay không
        if (status != Status.RUNNING) {
            System.err.println("Lỗi: Phiên đấu giá không ở trạng thái mở để nhận đấu giá!");
            return false;
        }
        
        // 2. Cấm người bán tự đấu giá
        if (bidder instanceof Seller && ((Seller) bidder).equals(bidProduct.seller)) {
            System.err.println("Lỗi: Người bán không thể đấu giá!");
            return false;
        }
        
        // 3. Kiểm tra xem phiên đấu giá hết thời gian chưa
        if (bidTimestamp.isAfter(endTime)) {
            System.err.println("Lỗi: Đã hết thời gian đấu giá!");
            return false;
        }
        
        if (bidAmount <= highestBid) {
            System.err.println("Lỗi: Giá đấu giá không hợp lệ!");
            return false;
        }
        
        // 4. Cập nhật người dẫn đầu và giá cao nhất
        highestBid = bidAmount;
        winningUser = bidder;
        
        // 5. Lưu lịch sử giao dịch
        BidTransaction transaction = new BidTransaction(bidProduct, bidder, bidAmount, bidTimestamp);
        bidTransactions.add(transaction);
        
        // 6. Kích hoạt Anti-snipping Algorithm
        handleAntiSniping(bidTimestamp);
        
        return true;
    }
    
    // Gia hạn
    public void handleAntiSniping(LocalDateTime bidTimestamp) {
        long secondsRemaining = ChronoUnit.SECONDS.between(bidTimestamp, endTime);
        if (secondsRemaining <= SNIPING_THRESHOLD_SECONDS) {
            this.endTime = this.endTime.plusSeconds(EXTENSION_SECONDS);
            System.err.println("Anti-snipping: Phiên đấu giá đã được gia hạn thêm " + EXTENSION_SECONDS + " giây.");
        }
    }
    
    // Chuyển trạng thái khi bắt đầu
    public void startAuction() {
        this.status = Status.RUNNING;
    }

    // Chuyển trạng thái khi kết thúc và công bố người thắng
    public boolean closeAuction() {
        this.status = Status.FINISHED;
        if (winningUser != null) {
            System.out.println("Phiên đấu giá kết thúc. Người thắng là: " + winningUser.getFullName());
            return true;
        } else {
            System.out.println("Phiên đấu giá kết thúc. Không có người thắng.");
            return false;
        }
    }

    // Chuyển sang trạng thái chấp nhận trả tiền
    public void acceptPayment() {
        this.status = Status.PAID;
        winningUser.moneyinFrozen -= highestBid;
        winningUser.successBidItem.add(bidProduct);
    }

    // Chuyển sang trạng thái hủy đấu giá
    public void cancelAuction() {
        this.status = Status.CANCELLED;
        winningUser.moneyinFrozen -= highestBid;
        winningUser.moneyOnWallet += highestBid;
        winningUser.failedBidItem.add(bidProduct);
    }
}
