package org.auctionfx.auctionbidsystemspringbootrework.repository;

import org.auctionfx.auctionbidsystemspringbootrework.entity.auction.Auction;
import org.auctionfx.auctionbidsystemspringbootrework.entity.item.Item;
import org.auctionfx.auctionbidsystemspringbootrework.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
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
            "AND a.transactionStatus IS NOT NULL " +
            "AND (a.winningUser IS NULL OR a.winningUser.userName != :userName OR a.transactionStatus = :failedStatus)")
    List<Auction> findLostAuctions(@Param("userName") String userName, @Param("failedStatus") TransactionStatus failedStatus);

    // 3. TÌM AUCTION THEO PRODUCT
    Optional<Auction> findByBidProduct(Item item);
}