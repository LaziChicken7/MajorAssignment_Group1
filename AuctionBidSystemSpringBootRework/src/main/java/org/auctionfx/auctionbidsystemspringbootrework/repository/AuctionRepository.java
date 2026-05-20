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

    // 1. LỌC GIAO DỊCH THÀNH CÔNG
    // Truyền tham số status kiểu Enum vào để Spring Boot tự hiểu
    @Query("SELECT a FROM Auction a WHERE a.winningUser.userName = :userName AND a.transactionStatus = :status")
    List<Auction> findWonAuctions(@Param("userName") String userName, @Param("status") TransactionStatus status);

    // 2. LỌC GIAO DỊCH THẤT BẠI
    // Lấy những phiên mà tôi đã đặt giá, ĐÃ KẾT THÚC, và (tôi trượt thầu HOẶC giao dịch bị FAILED/CANCELLED)
    @Query("SELECT DISTINCT a FROM Auction a JOIN a.bidTransactions b " +
            "WHERE b.bidder.userName = :userName " +
            // [ĐÃ SỬA]: Cho phép lấy nếu transactionStatus có dữ liệu HOẶC phiên đó bị CANCELLED
            "AND (a.transactionStatus IS NOT NULL OR a.status = :cancelledStatus) " +
            "AND (a.winningUser IS NULL OR a.winningUser.userName != :userName OR a.transactionStatus = :failedStatus)")
    List<Auction> findLostAuctions(
            @Param("userName") String userName,
            @Param("failedStatus") TransactionStatus failedStatus,
            @Param("cancelledStatus") AuctionStatus cancelledStatus // Thêm tham số này
    );

    // 3. TÌM AUCTION THEO PRODUCT
    Optional<Auction> findByBidProduct(Item item);

    // 4. Thêm annotation này để database "Khoá" dòng đấu giá này lại khi có người đặt giá.
    // Thằng khác (hoặc bot) bay vào phải Đứng Chờ thằng trước chạy xong transaction mới được đụng vào.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Auction a WHERE a.id = :id")
    Optional<Auction> findByIdWithLock(String id);

    // =========================================================
    // Đã bổ sung "bidProduct.imageUrls" để tránh lỗi N+1 Query
    // =========================================================
    @EntityGraph(attributePaths = {"bidProduct", "bidProduct.imageUrls", "seller"})
    List<Auction> findAll();

    @EntityGraph(attributePaths = {"bidProduct", "bidProduct.imageUrls", "seller"})
    List<Auction> findByBidProductNameContainingIgnoreCase(String keyword);

    // Thêm hàm lấy riêng sản phẩm của 1 người bán (Kèm luôn EntityGraph để tránh N+1)
    @EntityGraph(attributePaths = {"bidProduct", "seller"})
    @Query("SELECT a FROM Auction a WHERE a.seller.userName = :username")
    List<Auction> findBySellerUserName(@Param("username") String username);
}