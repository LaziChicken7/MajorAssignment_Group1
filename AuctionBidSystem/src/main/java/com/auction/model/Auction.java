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
    private Item bidProduct;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Status status;

    // QUẢN LÝ NGƯỜI DẪN ĐẦU VÀ LỊCH SỬ (THAY THẾ BẰNG DATABASE TRONG TƯƠNG LAI)
    private double highestBid;
    private User winningUser;
    private List<BidTransaction> bidTransactions; // Thay thế thành database trong tương lai

    // CẤU HÌNH CHO ANTI-SNIPING
    private static final int SNIPING_THRESHOLD_SECONDS = 10; // X giây cuối
    private static final int EXTENSION_SECONDS = 60; // Gia hạn Y giây

    // KHAI BÁO ĐỐI TƯỢNG
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
        // Kiểm tra xem phiên đấu giá có đang mở hay không
        if (status != Status.RUNNING) {
            System.err.println("Lỗi: Phiên đấu giá không ở trạng thái mở để nhận đấu giá!");
            return false;
        }

        // Kiểm tra xem phiên đấu giá hết thời gian chưa
        if (bidTimestamp.isAfter(endTime)) {
            System.err.println("Lỗi: Đã hết thời gian đấu giá!");
            return false;
        }

        if (bidAmount <= highestBid) {
            System.err.println("Lỗi: Giá đấu giá không hợp lệ!");
            return false;
        }

        // Cập nhật người dẫn đầu và giá cao nhất
        highestBid = bidAmount;
        winningUser = bidder;

        // Lưu lịch sử giao dịch
        BidTransaction transaction = new BidTransaction(bidProduct, bidder, bidAmount, bidTimestamp);
        bidTransactions.add(transaction);

        //Kích hoạt Anti-snipping Algorithm
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

}
