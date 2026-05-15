package org.auctionfx.auctionbidsystemspringbootrework.service;

import lombok.extern.slf4j.Slf4j;
import org.auctionfx.auctionbidsystemspringbootrework.entity.auction.Auction;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.User;
import org.auctionfx.auctionbidsystemspringbootrework.repository.AuctionRepository;
import org.auctionfx.auctionbidsystemspringbootrework.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class SearchService {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private UserRepository userRepository;

    // ==========================================
    // TÌM KIẾM SẢN PHẨM ĐẤU GIÁ
    // ==========================================
    public List<Auction> searchAuctions(String keyword) {
        log.info("SERVICE: Đang tìm kiếm sản phẩm đấu giá với từ khóa: [{}]", keyword);

        // Bạn có thể thêm các logic lọc nâng cao ở đây sau này (Ví dụ: Chỉ tìm sản phẩm đang RUNNING)
        List<Auction> results = auctionRepository.findByBidProductNameContainingIgnoreCase(keyword);

        log.debug("Tìm thấy {} sản phẩm khớp với từ khóa [{}]", results.size(), keyword);
        return results;
    }

    // ==========================================
    // TÌM KIẾM NGƯỜI DÙNG (BẠN BÈ / SELLER)
    // ==========================================
    public List<User> searchUsers(String keyword) {
        log.info("SERVICE: Đang tìm kiếm người dùng với từ khóa: [{}]", keyword);

        // =========================================================
        // ĐÃ SỬA: Gọi đúng tên hàm mới 'searchUsers' thay vì hàm dài loằng ngoằng cũ
        // =========================================================
        List<User> results = userRepository.searchUsers(keyword);

        log.debug("Tìm thấy {} người dùng khớp với từ khóa [{}]", results.size(), keyword);
        return results;
    }
}