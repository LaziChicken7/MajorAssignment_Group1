package org.auctionfx.auctionbidsystemspringbootrework.repository;

import org.auctionfx.auctionbidsystemspringbootrework.entity.auction.BidTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BidTransactionRepository extends JpaRepository<BidTransaction, String> {
    // Lấy danh sách giao dịch trả giá của 1 phiên đấu giá, sắp xếp theo thời gian
    List<BidTransaction> findByAuctionIdOrderByBidTimestampAsc(String auctionId);
}
