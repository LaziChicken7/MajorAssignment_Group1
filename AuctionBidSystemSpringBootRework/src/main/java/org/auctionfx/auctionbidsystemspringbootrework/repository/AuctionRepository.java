package org.auctionfx.auctionbidsystemspringbootrework.repository;

import org.auctionfx.auctionbidsystemspringbootrework.entity.auction.Auction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, String> {
    // 1. LỌC GIAO DỊCH THÀNH CÔNG:
    // Tìm các phiên đấu giá đã Kết thúc (FINISHED hoặc PAID) MÀ người thắng (winningUser) CHÍNH LÀ TÔI.
    @Query("SELECT a FROM Auction a WHERE a.winningUser.userName = :userName AND a.status IN ('FINISHED', 'PAID')")
    List<Auction> findWonAuctions(@Param("userName") String userName);

    // 2. LỌC GIAO DỊCH THẤT BẠI:
    // Tìm các phiên đấu giá đã Kết thúc, MÀ tôi CÓ THAM GIA ĐẶT GIÁ (JOIN bidTransactions),
    // NHƯNG người thắng lại KHÔNG PHẢI LÀ TÔI (Hoặc không có ai thắng).
    @Query("SELECT DISTINCT a FROM Auction a JOIN a.bidTransactions b " +
            "WHERE b.bidder.userName = :userName " +
            "AND (a.winningUser IS NULL OR a.winningUser.userName != :userName) " +
            "AND a.status IN ('FINISHED', 'PAID', 'CANCELLED')")
    List<Auction> findLostAuctions(@Param("userName") String userName);
}
