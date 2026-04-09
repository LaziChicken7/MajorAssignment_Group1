package com.auction.model.auction;

import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class NotFoundException extends Exception {
    public NotFoundException(String msg) {
        super(msg);
    }
}

// Quản lý toàn bộ các phiên đấu giá trong hệ thống
// Chịu trách nghiệm tạo phiên mới, lưu trữ danh sách phiên, tìm kiếm phiên và điều phối các hành động từ người dùng
public class AuctionManager {
    // 1. Áp dụng Singleton Pattern (là điểm vào duy nhất khi một User muốn thao tác với phiên đấu giá)
    private static AuctionManager instance;

    // Sử dụng ConcurrentHashMap để an toàn trong môi trường đa luồng
    private final Map<String, Auction> auctions;

    private int auctionCounter = 0;

    private AuctionManager() {
        this.auctions = new ConcurrentHashMap<>();
    }

    public static AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    // 2. Tạo mới một phiên đấu giá
    public synchronized Auction createAuction(Item item, Seller seller, LocalDateTime startTime, LocalDateTime endTime) {
        String auctionId = "AUC" + (++auctionCounter);
        Auction newAuction = new Auction(auctionId, item, seller, startTime, endTime);

        auctions.put(auctionId, newAuction);
        return newAuction;
    }

    // 3. Lấy ra một phiên đấu giá cụ thể
    public Auction getAuction(String auctionId) {
        return auctions.get(auctionId);
    }

    // 4. Ủy quyền hành động đặt giá xuống cho Auction xử lý
    public boolean placeBid(String auctionId, Bidder bidder, BigDecimal bidAmount, LocalDateTime bidTimestamp) throws NotFoundException, NotEnoughMoneyException {
        Auction auction = getAuction(auctionId);
        if (auction == null) {
            throw new NotFoundException("Phiên đấu giá không tồn tại");
        }
        try {
            return auction.placeBid(bidder, bidAmount, bidTimestamp);
        } catch (NotEnoughMoneyException e) {
            System.err.println(e.getMessage());
            throw new NotEnoughMoneyException(e.getMessage());
            // Chỉnh trong controller sau
        }
    }

    // Lấy toàn bộ danh sách phiên đấu giá (để hiển thị lên GUI)
    public Map<String, Auction> getAllAuctions() {
        return this.auctions;
    }
}
