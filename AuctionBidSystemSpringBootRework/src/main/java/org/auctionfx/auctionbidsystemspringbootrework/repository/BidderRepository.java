package org.auctionfx.auctionbidsystemspringbootrework.repository;

import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Bidder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BidderRepository extends JpaRepository<Bidder, String> {
    // Hàm tự động kiểm tra xem Số tài khoản đã tồn tại chưa
    boolean existsByBankAccountNumber(String bankAccountNumber);
}