package org.auctionfx.auctionbidsystemspringbootrework.repository;

import org.auctionfx.auctionbidsystemspringbootrework.entity.auction.Auction;
import org.auctionfx.auctionbidsystemspringbootrework.entity.auction.AutoBidConfig;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Bidder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AutoBidConfigRepository extends JpaRepository<AutoBidConfig, String> {

    // Tìm danh sách các bot đang được BẬT (isActive = true) của một phiên đấu giá cụ thể,
    // và sắp xếp theo thời gian cài đặt từ cũ đến mới (ưu tiên người cài đặt trước nếu đụng giá)
    List<AutoBidConfig> findByAuctionAndIsActiveTrueOrderByCreatedAtAsc(Auction auction);

    // Sửa Optional thành List để hứng hết các bản ghi bị trùng
    List<AutoBidConfig> findByAuctionAndBidder(Auction auction, Bidder bidder);
}