package org.auctionfx.auctionbidsystemspringboot.service;

import org.auctionfx.auctionbidsystemspringboot.entity.auction.Auction;
import org.auctionfx.auctionbidsystemspringboot.entity.item.Item;
import org.auctionfx.auctionbidsystemspringboot.entity.user.Bidder;
import org.auctionfx.auctionbidsystemspringboot.entity.user.Seller;
import org.auctionfx.auctionbidsystemspringboot.exception.NotEnoughMoneyException;
import org.auctionfx.auctionbidsystemspringboot.exception.NotFoundException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// Quản lý toàn bộ các phiên đấu giá trong hệ thống
// Chịu trách nghiệm tạo phiên mới, lưu trữ danh sách phiên, tìm kiếm phiên và điều phối các hành động từ người dùng
public class AuctionService {
    // 1. Áp dụng Singleton Pattern (là điểm vào duy nhất khi một User muốn thao tác với phiên đấu giá)
    // Từ khóa volatile ngăn chặn vấn đề Cache và Instruction Reordering của CPU
    private static volatile AuctionService instance;

    // Sử dụng ConcurrentHashMap để an toàn trong môi trường đa luồng
    private final Map<String, Auction> auctions;

    private AtomicInteger auctionCounter = new AtomicInteger(0);

    private AuctionService() {
        this.auctions = new ConcurrentHashMap<>();
    }

    public static AuctionService getInstance() {
        if (instance == null) {
            instance = new AuctionService();
        }
        return instance;
    }

    // 2. Tạo mới một phiên đấu giá
    public Auction createAuction(Item item, Seller seller, LocalDateTime startTime, LocalDateTime endTime) {
        // incrementAndGet() tự động cộng 1 một cách an toàn giữa các luồng
        String auctionId = "AUC" + auctionCounter.incrementAndGet();
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

        // Gọi xuống hàm placeBid của Auction (Nơi đã được bảo vệ bằng ReentrantLock)
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
