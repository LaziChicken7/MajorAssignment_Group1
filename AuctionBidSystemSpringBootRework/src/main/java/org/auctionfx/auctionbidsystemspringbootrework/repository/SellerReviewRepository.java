package org.auctionfx.auctionbidsystemspringbootrework.repository;

import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Seller;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.SellerReview;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SellerReviewRepository extends JpaRepository<SellerReview, String> {
    // Trả về List thuần túy (để JavaFX dễ đọc) nhưng nhận Pageable để cắt dòng
    List<SellerReview> findBySellerOrderByCreatedAtDesc(Seller seller, Pageable pageable);

    boolean existsBySellerAndReviewer(Seller seller, org.auctionfx.auctionbidsystemspringbootrework.entity.user.User reviewer);
}
