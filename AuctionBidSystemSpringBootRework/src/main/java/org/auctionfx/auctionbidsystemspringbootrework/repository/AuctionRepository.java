package org.auctionfx.auctionbidsystemspringbootrework.repository;

import jakarta.persistence.LockModeType;
import org.auctionfx.auctionbidsystemspringbootrework.entity.auction.Auction;
import org.auctionfx.auctionbidsystemspringbootrework.entity.item.Item;
import org.auctionfx.auctionbidsystemspringbootrework.enums.AuctionStatus;
import org.auctionfx.auctionbidsystemspringbootrework.enums.TransactionStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, String> {

    // 1. LỌC GIAO DỊCH THÀNH CÔNG (Đã bổ sung EntityGraph chống N+1)
    @EntityGraph(attributePaths = {"bidProduct", "bidProduct.imageUrls", "seller"})
    @Query("SELECT a FROM Auction a WHERE a.winningUser.userName = :userName AND a.transactionStatus = :status")
    List<Auction> findWonAuctions(@Param("userName") String userName, @Param("status") TransactionStatus status);

    // 2. LỌC GIAO DỊCH THẤT BẠI (Đã bổ sung EntityGraph chống N+1)
    @EntityGraph(attributePaths = {"bidProduct", "bidProduct.imageUrls", "seller"})
    @Query("SELECT DISTINCT a FROM Auction a JOIN a.bidTransactions b " +
            "WHERE b.bidder.userName = :userName " +
            "AND (a.transactionStatus IS NOT NULL OR a.status = :cancelledStatus) " +
            "AND (a.winningUser IS NULL OR a.winningUser.userName != :userName OR a.transactionStatus = :failedStatus)")
    List<Auction> findLostAuctions(
            @Param("userName") String userName,
            @Param("failedStatus") TransactionStatus failedStatus,
            @Param("cancelledStatus") AuctionStatus cancelledStatus
    );

    // 3. TÌM AUCTION THEO PRODUCT
    Optional<Auction> findByBidProduct(Item item);

    // 4. Khoá dòng đấu giá (Lock)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Auction a WHERE a.id = :id")
    Optional<Auction> findByIdWithLock(String id);

    // 5. Lấy tất cả (Đã chuẩn)
    @EntityGraph(attributePaths = {"bidProduct", "bidProduct.imageUrls", "seller"})
    List<Auction> findAll();

    // 6. Tìm kiếm (Đã chuẩn)
    @EntityGraph(attributePaths = {"bidProduct", "bidProduct.imageUrls", "seller"})
    List<Auction> findByBidProductNameContainingIgnoreCase(String keyword);

    // =========================================================
    // 7. LẤY SẢN PHẨM CỦA TÔI (ĐÃ FIX LỖI THIẾU ẢNH GÂY LAG SERVER)
    // Thêm "bidProduct.imageUrls" vào đây để lấy ảnh ngay trong 1 lần Query!
    // =========================================================
    @EntityGraph(attributePaths = {"bidProduct", "bidProduct.imageUrls", "seller"})
    @Query("SELECT a FROM Auction a WHERE a.seller.userName = :username")
    List<Auction> findBySellerUserName(@Param("username") String username);
}