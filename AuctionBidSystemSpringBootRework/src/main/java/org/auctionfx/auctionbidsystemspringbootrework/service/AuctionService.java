package org.auctionfx.auctionbidsystemspringbootrework.service;

import org.auctionfx.auctionbidsystemspringbootrework.dto.request.AuctionCreationRequest;
import org.auctionfx.auctionbidsystemspringbootrework.entity.auction.Auction;
import org.auctionfx.auctionbidsystemspringbootrework.entity.auction.BidTransaction;
import org.auctionfx.auctionbidsystemspringbootrework.entity.item.Item;
import org.auctionfx.auctionbidsystemspringbootrework.entity.notification.Notification;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Bidder;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.User;
import org.auctionfx.auctionbidsystemspringbootrework.enums.AuctionStatus;
import org.auctionfx.auctionbidsystemspringbootrework.enums.NotificationType;
import org.auctionfx.auctionbidsystemspringbootrework.exception.AuctionException;
import org.auctionfx.auctionbidsystemspringbootrework.exception.ErrorCode;
import org.auctionfx.auctionbidsystemspringbootrework.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AuctionService {
    @Autowired private AuctionRepository auctionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BidTransactionRepository bidTransactionRepository;
    @Autowired private PaymentService paymentService; // Gọi bếp phó (Payment) hỗ trợ
    @Autowired private ItemRepository itemRepository;
    @Autowired private NotificationRepository notificationRepository;

    // Cấu hình thuật toán Anti-Snipping
    private static final int SNIPING_THRESHOLD_SECONDS = 10; // Đấu giá trong 10s cuối
    private static final int EXTENSION_SECONDS = 60; // Thì gia hạn thêm 60s

    // 1. Chức năng đặt giá (Place Bid) - Quan trọng nhất
    @Transactional(rollbackFor = Exception.class) // Nếu có lỗi xảy ra, toàn bộ tiền sẽ được rollback lại như cũ
    public String placeBid(String auctionId, String bidderUserName, BigDecimal bidAmount) {
        // 1. Tìm phiên đấu giá
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionException(ErrorCode.AUCTION_NOT_FOUND));

        // 2. Tìm người đặt giá
        User user = userRepository.findByUserName(bidderUserName);
        if (user == null || !(user instanceof Bidder)) {
            throw new AuctionException(ErrorCode.USER_INVALID);
        }
        Bidder bidder = (Bidder) user;
        LocalDateTime bidTimestamp = LocalDateTime.now();

        // 3. Kiểm tra điều kiện
        if (auction.getStatus() != AuctionStatus.RUNNING) {
            throw new AuctionException(ErrorCode.AUCTION_NOT_RUNNING);
        }
        if (bidder.getId().equals(auction.getSeller().getId())) {
            throw new AuctionException(ErrorCode.AUCTION_BIDDER_INVALID);
        }
        if (bidTimestamp.isAfter(auction.getEndTime())) {
            throw new AuctionException(ErrorCode.AUCTION_NOT_RUNNING);
        }
        if (bidAmount.compareTo(auction.getHighestBid()) <= 0) {
            throw new RuntimeException("Money must be higher than now (" + auction.getHighestBid() + ")");
        }

        // 4. Xử lý tiền (Hoàn tiền cho người cũ, đóng hoàn tiền cho người mới)
        Bidder previousWinner = auction.getWinningUser();
        BigDecimal previousBid = auction.getHighestBid();

        // Nếu có người dẫn đầu trước đó -> Trả lại tiền cho họ
        if (previousWinner != null) {
            paymentService.unFreezeMoney(previousWinner.getId(), previousBid);
        }

        // Đóng băng tiền của người đặt giá mới
        paymentService.freezeMoney(bidder.getId(), bidAmount);

        // 5. Cập nhật phiên đấu giá
        auction.setHighestBid(bidAmount);
        auction.setWinningUser(bidder);

        // 6. Thuật toán ANTI-SNIPPING
        long secondsRemaining = ChronoUnit.SECONDS.between(bidTimestamp, auction.getEndTime());
        boolean isExtended = false;
        if (secondsRemaining <= SNIPING_THRESHOLD_SECONDS) {
            auction.setEndTime(auction.getEndTime().plusSeconds(EXTENSION_SECONDS));
            isExtended = true;
        }

        // 7. Lưu lịch sử giao dịch
        BidTransaction transaction = new BidTransaction();
        transaction.setAuction(auction);
        transaction.setBidder(bidder);
        transaction.setBidAmount(bidAmount);
        transaction.setBidTimestamp(bidTimestamp);

        bidTransactionRepository.save(transaction);
        auctionRepository.save(auction);

        String result = "Bid successfully with money: " + bidAmount;
        if (isExtended) {
            result += "\n(Anti-Snipping system activated: Extended 60 seconds)";
        }
        return result;
    }

    // 2. Bắt đầu và kết thúc phiên đấu giá
    @Transactional
    public void startAuction(String auctionId) {
        Auction auction = auctionRepository.findById(auctionId).orElseThrow();
        auction.setStatus(AuctionStatus.RUNNING);
        auctionRepository.save(auction);
    }

    @Transactional
    public String closeAuction(String auctionId) {
        Auction auction = auctionRepository.findById(auctionId).orElseThrow();
        auction.setStatus(AuctionStatus.FINISHED);
        auctionRepository.save(auction);

        if (auction.getWinningUser() != null) {
            // ============== CODE THÊM MỚI Ở ĐÂY ==============
            Notification notif = new Notification();
            notif.setUser(auction.getWinningUser()); // Gửi cho người thắng
            notif.setAuction(auction);
            notif.setType(NotificationType.PAYMENT_VERIFICATION);
            // Cắt 4 ký tự ID SP (ví dụ ITEM -> IT) làm mã hiển thị SP01
            notif.setTitle("Xác thực giao dịch: SP" + auction.getBidProduct().getId().substring(0, 4).toUpperCase());
            notif.setDescription(auction.getBidProduct().getName() + " - Giá tiền: " + auction.getHighestBid() + " VND");

            notificationRepository.save(notif);
            // =================================================
            return "Session ended! Winner is: " + auction.getWinningUser().getFullName();
        }
        return "Session ended! No one winner";
    }

    // 3. Chấp nhận trả tiền và từ chối trả tiền
    @Transactional
    public void acceptPayment(String auctionId) {
        Auction auction = auctionRepository.findById(auctionId).orElseThrow();

        if (auction.getStatus() != AuctionStatus.FINISHED || auction.getWinningUser() == null) {
            throw new AuctionException(ErrorCode.CONDITION_ACCEPT_PAYMENT_INVALID);
        }

        // Chuyển tiền từ người mua sang người bán
        paymentService.transferMoney(
                auction.getWinningUser().getId(),
                auction.getSeller().getId(),
                auction.getHighestBid()
        );

        auction.setStatus(AuctionStatus.PAID);
        auctionRepository.save(auction);
    }

    @Transactional
    public void declinePayment(String auctionId) {
        Auction auction = auctionRepository.findById(auctionId).orElseThrow();

        if (auction.getStatus() != AuctionStatus.FINISHED || auction.getWinningUser() == null) {
            throw new AuctionException(ErrorCode.CONDITION_ACCEPT_PAYMENT_INVALID);
        }

        // Trả lại tiền cho người bán
        paymentService.unFreezeMoney(auction.getWinningUser().getId(), auction.getHighestBid());

        auction.setStatus(AuctionStatus.CANCELLED);
        auctionRepository.save(auction);
    }

    @Transactional
    public void cancelAuction(String auctionId) {
        Auction auction = auctionRepository.findById(auctionId).orElseThrow();

        if (auction.getStatus() != AuctionStatus.RUNNING || auction.getWinningUser() == null) {
            throw new AuctionException(ErrorCode.CONDITION_CANCEL_AUCTION_INVALID);
        }

        // Trả lại tiền cho người đang dẫn đầu (nếu có)
        paymentService.unFreezeMoney(auction.getWinningUser().getId(), auction.getHighestBid());

        auction.setStatus(AuctionStatus.CANCELLED);
        auctionRepository.save(auction);
    }

    // 4. Tạo sản phẩm đấu giá
    @Transactional(rollbackFor = Exception.class)
    public String createAuction(AuctionCreationRequest request) {
        Item item = itemRepository.findById(request.getItemId())
                .orElseThrow(() -> new AuctionException(ErrorCode.ITEM_NOT_FOUND));

        Auction auction = new Auction();
        auction.setBidProduct(item);
        auction.setSeller(item.getSeller());
        auction.setStartTime(LocalDateTime.now());
        auction.setEndTime(request.getEndTime());
        auction.setHighestBid(item.getStartPrice()); // Giá khởi điểm

        auctionRepository.save(auction);
        return "Create Auction item successfully! ID: " + auction.getId();
    }

    // Lấy danh sách tất cả các phiên đấu giá
    public List<Auction> getAllAuctions() {
        return auctionRepository.findAll();
    }
}
/*
1. Quản lý Transaction (@Transactional):
Giả sử trong hàm placeBid, bước đóng băng tiền chạy thành công, nhưng bước lưu lịch sử giao dịch bị lỗi mạng hoặc sập nguồn.
Ở code cũ, tiền của user sẽ bị trừ mất tiêu. Ở Spring Boot, nhờ có @Transactional(rollbackFor = Exception.class),
Database sẽ "khôi phục" (Rollback) lại toàn bộ như chưa từng có cuộc chia ly. Không ai bị mất tiền oan!

2. Loại bỏ ReentrantLock:
Database MySQL có cơ chế khóa riêng của nó. Nếu 2 người bấm gửi request cùng một phần nghìn giây,
MySQL sẽ xếp hàng chúng lại.
 */