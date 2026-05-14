package org.auctionfx.auctionbidsystemspringbootrework.service;

import lombok.extern.slf4j.Slf4j;
import org.auctionfx.auctionbidsystemspringbootrework.dto.request.AuctionCreationRequest;
import org.auctionfx.auctionbidsystemspringbootrework.entity.auction.Auction;
import org.auctionfx.auctionbidsystemspringbootrework.entity.auction.AutoBidConfig;
import org.auctionfx.auctionbidsystemspringbootrework.entity.auction.BidTransaction;
import org.auctionfx.auctionbidsystemspringbootrework.entity.item.Item;
import org.auctionfx.auctionbidsystemspringbootrework.entity.notification.Notification;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Bidder;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.User;
import org.auctionfx.auctionbidsystemspringbootrework.enums.AuctionStatus;
import org.auctionfx.auctionbidsystemspringbootrework.enums.NotificationType;
import org.auctionfx.auctionbidsystemspringbootrework.enums.TransactionStatus;
import org.auctionfx.auctionbidsystemspringbootrework.exception.AuctionException;
import org.auctionfx.auctionbidsystemspringbootrework.exception.ErrorCode;
import org.auctionfx.auctionbidsystemspringbootrework.exception.UserException;
import org.auctionfx.auctionbidsystemspringbootrework.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Slf4j // Kích hoạt bộ ghi log của Lombok
public class AuctionService {
    @Autowired private AuctionRepository auctionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BidTransactionRepository bidTransactionRepository;
    @Autowired private PaymentService paymentService; // Gọi bếp phó (Payment) hỗ trợ
    @Autowired private ItemRepository itemRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private AutoBidConfigRepository autoBidConfigRepository;

    // Cấu hình thuật toán Anti-Snipping
    private static final int SNIPING_THRESHOLD_SECONDS = 10; // Đấu giá trong 10s cuối
    private static final int EXTENSION_SECONDS = 60; // Thì gia hạn thêm 60s

    // Bạn nên định nghĩa 1 BƯỚC GIÁ (Ví dụ: mỗi lần Autobid sẽ tự động cộng thêm 10.000 VND so với giá hiện tại)
    // (Note: Sẽ lấy stepPrice động từ Auction, nếu null thì mặc định lấy 10000 như comment của bạn)
    private static final BigDecimal DEFAULT_BID_STEP = new BigDecimal("10000");

    // 1. Cài đặt AutoBid
    @Transactional(rollbackFor = Exception.class)
    public String setupAutoBid(String auctionId, String bidderUserName, BigDecimal maxAmount) {
        log.info("SERVICE: Bắt đầu xử lý luồng thêm Autobid. AuctionID={}, Username={}, Amount={}", auctionId, bidderUserName, maxAmount);

        // 1. Tìm phiên đấu giá
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> {
                    log.error("Lỗi: Không tìm thấy phiên đấu giá [{}]", auctionId);
                    return new AuctionException(ErrorCode.AUCTION_NOT_FOUND);
                });

        // 2. TÌM VÀ KIỂM TRA NGƯỜI DÙNG (Cập nhật kiểm tra an toàn)
        User user = userRepository.findByUserName(bidderUserName);
        if (user == null || !(user instanceof Bidder)) {
            log.error("Lỗi: Người dùng [{}] không tồn tại hoặc không phải là Bidder", bidderUserName);
            throw new AuctionException(ErrorCode.USER_INVALID);
        }
        Bidder bidder = (Bidder) user;

        // ========================================================================
        // FIX: KIỂM TRA SELLER KHÔNG ĐƯỢC TỰ CÀI BOT CHO SẢN PHẨM CỦA MÌNH
        // ========================================================================
        if (bidder.getId().equals(auction.getSeller().getId())) {
            log.warn("Cảnh báo: Seller [{}] cố tình cài đặt Bot tự đấu giá sản phẩm của chính mình!", bidder.getUserName());
            throw new AuctionException(ErrorCode.AUCTION_BIDDER_INVALID);
        }

        // Kiểm tra xem tài khoản có bị cấm không
        if (bidder.isBanned()) {
            log.warn("Cảnh báo: Tài khoản [{}] đã bị cấm nhưng vẫn cố cài Autobid", bidderUserName);
            throw new UserException(ErrorCode.USER_BANNED);
        }

        // 3. Kiểm tra maxAmount có lớn hơn giá hiện tại không
        if (maxAmount.compareTo(auction.getHighestBid()) <= 0) {
            log.warn("Cảnh báo: Giá nhập ({}) nhỏ hơn hoặc bằng giá hiện tại ({})", maxAmount, auction.getHighestBid());
            throw new RuntimeException("Money autobid must be higher than now (" + auction.getHighestBid() + ")");
        }

        // Lưu cấu hình vào DB
        AutoBidConfig config = new AutoBidConfig();
        config.setAuction(auction);
        config.setBidder(bidder);
        config.setMaxBidAmount(maxAmount);
        autoBidConfigRepository.save(config);

        // KÍCH HOẠT AUTOBID NGAY LẬP TỨC (Nhỡ đâu lúc setup thì người khác đã đặt giá cao hơn rồi)
        triggerAutoBidProcess(auction);

        return "Setup Auto-bid successfully!";
    }

    // 2. Chức năng đặt giá (Place Bid)
    @Transactional(rollbackFor = Exception.class) // Nếu có lỗi xảy ra, toàn bộ tiền sẽ được rollback lại như cũ
    public String placeBid(String auctionId, String bidderUserName, BigDecimal bidAmount) {
        log.info("SERVICE: Bắt đầu xử lý luồng đặt giá. AuctionID={}, Username={}, Amount={}", auctionId, bidderUserName, bidAmount);

        // 1. Tìm phiên đấu giá
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> {
                    log.error("Lỗi: Không tìm thấy phiên đấu giá [{}]", auctionId);
                    return new AuctionException(ErrorCode.AUCTION_NOT_FOUND);
                });

        // 2. Tìm người đặt giá
        User user = userRepository.findByUserName(bidderUserName);
        if (user == null || !(user instanceof Bidder)) {
            log.error("Lỗi: Người dùng [{}] không tồn tại hoặc không phải là Bidder", bidderUserName);
            throw new AuctionException(ErrorCode.USER_INVALID);
        }
        Bidder bidder = (Bidder) user;
        LocalDateTime bidTimestamp = LocalDateTime.now();

        // 3. Kiểm tra điều kiện
        if (auction.getStatus() != AuctionStatus.RUNNING) {
            log.warn("Cảnh báo: Từ chối đặt giá. Phiên đấu giá [{}] không ở trạng thái RUNNING", auctionId);
            throw new AuctionException(ErrorCode.AUCTION_NOT_RUNNING);
        }
        if (bidder.getId().equals(auction.getSeller().getId())) {
            log.warn("Cảnh báo: Seller [{}] cố tình tự đặt giá sản phẩm của chính mình!", bidder.getUserName());
            throw new AuctionException(ErrorCode.AUCTION_BIDDER_INVALID);
        }
        if (bidTimestamp.isAfter(auction.getEndTime())) {
            log.warn("Cảnh báo: Phiên đấu giá [{}] đã hết giờ!", auctionId);
            throw new AuctionException(ErrorCode.AUCTION_NOT_RUNNING);
        }
        if (bidder.isBanned()) {
            log.warn("Cảnh báo: Tài khoản [{}] đã bị cấm nhưng vẫn cố đặt giá", bidderUserName);
            throw new UserException(ErrorCode.USER_BANNED);
        }
        if (bidAmount.compareTo(auction.getHighestBid()) <= 0) {
            log.warn("Cảnh báo: Giá nhập ({}) nhỏ hơn hoặc bằng giá hiện tại ({})", bidAmount, auction.getHighestBid());
            throw new AuctionException(ErrorCode.BID_AMOUNT_INVALID);
        }

        // Đã tách phần lõi xử lý tiền và cập nhật trạng thái ra hàm riêng để dùng chung
        boolean isExtended = executeInternalBid(auction, bidder, bidAmount);

        // --- THÊM DÒNG NÀY: Kích hoạt hệ thống chạy kiểm tra Autobid ngay sau khi có giá mới ---
        triggerAutoBidProcess(auction);

        String result = "Bid successfully with money: " + bidAmount;
        if (isExtended) {
            result += "\n(Anti-Snipping system activated: Extended 60 seconds)";
        }

        log.info("Giao dịch đặt giá thành công. Trạng thái hiện tại: Người thắng mới là [{}]", bidder.getUserName());
        return result;
    }

    // Để tránh việc hàm placeBid gọi triggerAutoBidProcess, rồi triggerAutoBidProcess lại gọi placeBid tạo thành một mớ bòng bong vô tận, tách cái lõi của placeBid ra thành một hàm private executeInternalBid.
    // Đây là hàm dùng chung cho cả Người Đặt và Bot Đặt
    private boolean executeInternalBid(Auction auction, Bidder bidder, BigDecimal bidAmount) {
        // 1. Xử lý tiền (Hoàn tiền cho người cũ, đóng hoàn tiền cho người mới)
        Bidder previousWinner = auction.getWinningUser();
        BigDecimal previousBid = auction.getHighestBid();

        // 2. Rút lại tiền người cũ
        // Nếu có người dẫn đầu trước đó -> Trả lại tiền cho họ
        if (previousWinner != null && !previousWinner.getId().equals(bidder.getId())) {
            log.info("Thực hiện hoàn trả tiền {} VND cho người dẫn đầu cũ [{}]", previousBid, previousWinner.getUserName());
            paymentService.unFreezeMoney(previousWinner.getId(), previousBid);
        }

        // 3. Trừ tiền người mới
        // Đóng băng tiền của người đặt giá mới
        log.info("Thực hiện đóng băng {} VND của Bidder mới [{}]", bidAmount, bidder.getUserName());
        paymentService.freezeMoney(bidder.getId(), bidAmount);

        // 4. Cập nhật Auction
        auction.setHighestBid(bidAmount);
        auction.setWinningUser(bidder);

        // 5. Thuật toán ANTI-SNIPPING
        boolean isExtended = false;
        long secondsRemaining = ChronoUnit.SECONDS.between(LocalDateTime.now(), auction.getEndTime());
        if (secondsRemaining <= SNIPING_THRESHOLD_SECONDS) {
            log.info("HỆ THỐNG ANTI-SNIPPING KÍCH HOẠT: Chỉ còn {} giây. Gia hạn thêm {} giây cho phiên [{}]", secondsRemaining, EXTENSION_SECONDS, auction.getId());
            auction.setEndTime(auction.getEndTime().plusSeconds(EXTENSION_SECONDS));
            isExtended = true;
        }

        // 6. Lưu lịch sử giao dịch
        BidTransaction transaction = new BidTransaction();
        transaction.setAuction(auction);
        transaction.setBidder(bidder);
        transaction.setBidAmount(bidAmount);
        transaction.setBidTimestamp(LocalDateTime.now());

        bidTransactionRepository.save(transaction);
        auctionRepository.save(auction);

        return isExtended;
    }

    // Kích hoạt AutoBid
    private void triggerAutoBidProcess(Auction auction) {
        boolean keepBidding = true;

        // Vòng lặp: Bọn bot sẽ đánh nhau đến khi không ai có thể ra giá được nữa
        while (keepBidding) {
            keepBidding = false;

            // Lấy danh sách các cấu hình AutoBid đang ACTIVE của phiên này (sắp xếp theo thời gian setup cũ nhất -> ưu tiên)
            List<AutoBidConfig> autoBids = autoBidConfigRepository.findByAuctionAndIsActiveTrueOrderByCreatedAtAsc(auction);

            // Xác định bước giá (dùng động nếu có, hoặc mặc định 10000)
            BigDecimal stepPrice = auction.getStepPrice() != null ? auction.getStepPrice() : DEFAULT_BID_STEP;

            for (AutoBidConfig autoBid : autoBids) {
                // Nếu người này ĐANG là người dẫn đầu rồi thì không tự bid đè lên chính mình
                if (auction.getWinningUser() != null && auction.getWinningUser().getId().equals(autoBid.getBidder().getId())) {
                    continue;
                }

                // Tính mức giá tiếp theo cần đặt (Giá cao nhất hiện tại + Bước giá)
                BigDecimal nextBidAmount = auction.getHighestBid().add(stepPrice);

                // Nếu mức giá tiếp theo vẫn NẰM TRONG khả năng chi trả của AutoBid (<= maxAmount)
                if (nextBidAmount.compareTo(autoBid.getMaxBidAmount()) <= 0) {
                    log.info("AUTOBID: Kích hoạt bot của [{}] đánh giá [{}].", autoBid.getBidder().getUserName(), nextBidAmount);

                    // Gọi lại hàm placeBid để bot thực hiện đặt giá (như con người)
                    // Lưu ý: Tách logic xử lý bên trong placeBid ra 1 private method để tránh vòng lặp gọi lại triggerAutoBidProcess
                    executeInternalBid(auction, autoBid.getBidder(), nextBidAmount);

                    // Đã có giá mới, vòng lặp while sẽ chạy lại từ đầu để các bot khác đánh trả
                    keepBidding = true;
                    break; // Thoát vòng for để lấy lại danh sách mới nhất
                } else {
                    // Tiền Max đã không đọ lại được nữa -> Tắt bot của người này đi
                    log.info("AUTOBID: Bot của [{}] đã đuối sức (Max: {}). Tắt bot.", autoBid.getBidder().getUserName(), autoBid.getMaxBidAmount());
                    autoBid.setActive(false);
                    autoBidConfigRepository.save(autoBid);
                }
            }
        }
    }

    // 3. Bắt đầu và kết thúc phiên đấu giá
    @Transactional
    public void startAuction(String auctionId) {
        log.info("Bắt đầu khởi chạy thủ công phiên đấu giá[{}]", auctionId);
        Auction auction = auctionRepository.findById(auctionId).orElseThrow();
        auction.setStatus(AuctionStatus.RUNNING);
        auctionRepository.save(auction);
    }

    @Transactional
    public String closeAuction(String auctionId) {
        log.info("Bắt đầu quá trình ĐÓNG phiên đấu giá [{}]", auctionId);
        Auction auction = auctionRepository.findById(auctionId).orElseThrow();
        auction.setStatus(AuctionStatus.FINISHED);
        String shortId = auction.getBidProduct().getId().substring(0, 4).toUpperCase();

        if (auction.getWinningUser() != null) {
            log.info("Phiên đấu giá [{}] kết thúc thành công. Người thắng: [{}]", auctionId, auction.getWinningUser().getUserName());
            // 1. Thông báo cho người mua xác thực thanh toán
            Notification winnerNotif = new Notification();
            winnerNotif.setUser(auction.getWinningUser());
            winnerNotif.setAuction(auction);
            winnerNotif.setType(NotificationType.PAYMENT_VERIFICATION);
            winnerNotif.setTitle("Xác thực giao dịch: SP" + shortId);
            winnerNotif.setDescription(auction.getBidProduct().getName() + " - Giá tiền: " + auction.getHighestBid() + " VND");
            notificationRepository.save(winnerNotif);

            // 2. BỔ SUNG: Thông báo cho Người bán (Biết đã có người thắng)
            Notification sellerNotif = new Notification();
            sellerNotif.setUser(auction.getSeller());
            sellerNotif.setAuction(auction);
            sellerNotif.setType(NotificationType.AUCTION_SUCCESS); // Cứ coi là thông báo thông tin
            sellerNotif.setTitle("Phiên đấu giá kết thúc: SP" + shortId);
            sellerNotif.setDescription("Người thắng: " + auction.getWinningUser().getFullName() + " với giá " + auction.getHighestBid() + " VND. Đang chờ người mua xác thực thanh toán.");
            notificationRepository.save(sellerNotif);

            auctionRepository.save(auction);
            return "Session ended! Winner is: " + auction.getWinningUser().getFullName();
        } else {
            log.info("Phiên đấu giá [{}] kết thúc nhưng Ế (Không ai đấu giá)", auctionId);
            auction.setTransactionStatus(TransactionStatus.FAILED);

            // 3. BỔ SUNG: Thông báo cho Người bán (Báo ế / Không ai mua)
            Notification sellerFailNotif = new Notification();
            sellerFailNotif.setUser(auction.getSeller());
            sellerFailNotif.setAuction(auction);
            sellerFailNotif.setType(NotificationType.AUCTION_FAILED);
            sellerFailNotif.setTitle("Phiên đấu giá kết thúc: SP" + shortId);
            sellerFailNotif.setDescription("Sản phẩm " + auction.getBidProduct().getName() + " đã hết thời gian nhưng không có ai tham gia trả giá.");
            notificationRepository.save(sellerFailNotif);

            auctionRepository.save(auction);
            return "Session ended! No one winner";
        }
    }

    // 4. Chấp nhận trả tiền và từ chối trả tiền
    @Transactional
    public void acceptPayment(String auctionId) {
        log.info("Người mua đã CHẤP NHẬN thanh toán cho phiên [{}]", auctionId);
        Auction auction = auctionRepository.findById(auctionId).orElseThrow();
        if (auction.getStatus() != AuctionStatus.FINISHED || auction.getWinningUser() == null) {
            log.error("Lỗi: Không đủ điều kiện để thanh toán cho phiên [{}]", auctionId);
            throw new AuctionException(ErrorCode.CONDITION_ACCEPT_PAYMENT_INVALID);
        }
        paymentService.transferMoney(auction.getWinningUser().getId(), auction.getSeller().getId(), auction.getHighestBid());

        auction.setStatus(AuctionStatus.PAID);
        // GÁN SUCCESS KHI THANH TOÁN THÀNH CÔNG
        auction.setTransactionStatus(TransactionStatus.SUCCESS);
        auctionRepository.save(auction);
        log.info("Đã chuyển thành công {} VND cho người bán", auction.getHighestBid());
    }

    @Transactional
    public void declinePayment(String auctionId) {
        log.warn("Người mua TỪ CHỐI thanh toán cho phiên [{}]", auctionId);
        Auction auction = auctionRepository.findById(auctionId).orElseThrow();
        if (auction.getStatus() != AuctionStatus.FINISHED || auction.getWinningUser() == null) {
            log.error("Lỗi: Không đủ điều kiện hủy thanh toán cho phiên [{}]", auctionId);
            throw new AuctionException(ErrorCode.CONDITION_ACCEPT_PAYMENT_INVALID);
        }
        paymentService.unFreezeMoney(auction.getWinningUser().getId(), auction.getHighestBid());

        auction.setStatus(AuctionStatus.CANCELLED);
        // GÁN FAILED KHI TỪ CHỐI THANH TOÁN
        auction.setTransactionStatus(TransactionStatus.FAILED);
        auctionRepository.save(auction);
        log.info("Đã hoàn lại số tiền {} VND cho người mua bị từ chối", auction.getHighestBid());
    }

    @Transactional
    public void cancelAuction(String auctionId) {
        log.warn("Hệ thống (Hoặc Admin) yêu cầu HỦY phiên đấu giá [{}]", auctionId);
        Auction auction = auctionRepository.findById(auctionId).orElseThrow();

        if (auction.getStatus() != AuctionStatus.RUNNING || auction.getWinningUser() == null) {
            throw new AuctionException(ErrorCode.CONDITION_CANCEL_AUCTION_INVALID);
        }

        // Trả lại tiền cho người đang dẫn đầu (nếu có)
        log.info("Đang tiến hành hoàn trả tiền cho người đang dẫn đầu (nếu có)...");
        paymentService.unFreezeMoney(auction.getWinningUser().getId(), auction.getHighestBid());

        auction.setStatus(AuctionStatus.CANCELLED);
        auctionRepository.save(auction);
        log.info("Đã hủy phiên đấu giá thành công");
    }

    // 5. Tạo sản phẩm đấu giá
    @Transactional(rollbackFor = Exception.class)
    public String createAuction(AuctionCreationRequest request) {
        log.info("Bắt đầu tạo phiên đấu giá cho sản phẩm ID [{}]", request.getItemId());
        Item item = itemRepository.findById(request.getItemId())
                .orElseThrow(() -> new AuctionException(ErrorCode.ITEM_NOT_FOUND));

        Auction auction = new Auction();
        auction.setBidProduct(item);
        auction.setSeller(item.getSeller());
        // SỬA DÒNG NÀY: Lấy thời gian bắt đầu từ Request thay vì now()
        auction.setStartTime(request.getStartTime());
        auction.setEndTime(request.getEndTime());
        auction.setHighestBid(item.getStartPrice()); // Giá khởi điểm

        auctionRepository.save(auction);
        log.info("Tạo phiên đấu giá thành công. AuctionID={}", auction.getId());
        return "Create Auction item successfully! ID: " + auction.getId();
    }

    // Lấy danh sách tất cả các phiên đấu giá
    public List<Auction> getAllAuctions() {
        return auctionRepository.findAll();
    }

    // API Lấy dữ liệu biểu đồ giá theo thời gian thực
    public List<BidTransaction> getPriceChart(String auctionId) {
        // Nếu ID truyền lên bị rỗng hoặc null
        if (auctionId == null || auctionId.trim().isEmpty()) {
            log.error("Lỗi: Auction ID truyền vào bị trống hoặc null");
            throw new IllegalArgumentException("Auction ID không hợp lệ");
        }

        // Kiểm tra xem phiên đấu giá có tồn tại thực sự trong DB không
        if (!auctionRepository.existsById(auctionId)) {
            log.error("Lỗi: Không tìm thấy phiên đấu giá [{}] trong CSDL", auctionId);
            throw new AuctionException(ErrorCode.AUCTION_NOT_FOUND);
        }

        try {
            // lấy danh sách, sắp theo thời gian cũ đến mới
            List<BidTransaction> chartData = bidTransactionRepository.findByAuctionIdOrderByBidTimestampAsc(auctionId);

            // trả về mảng rỗng nếu chưa có ai bid
            if (chartData == null) {
                log.debug("Biểu đồ rỗng: Chưa có lượt đấu giá nào cho phiên [{}]", auctionId);
                return new ArrayList<>();
            }
            log.debug("Lấy thành công {} lượt giao dịch cho biểu đồ phiên [{}]", chartData.size(), auctionId);
            return chartData;
        } catch (Exception e) {
            // Bắt lỗi hệ thống
            log.error("LỖI HỆ THỐNG: Lỗi truy xuất cơ sở dữ liệu khi vẽ biểu đồ phiên [{}]: ", auctionId, e);
            throw new AuctionException(ErrorCode.BARCHART_CONNECT_FAILURE);
        }
    }

    // Tự động quét Auction
    @Scheduled(fixedRate = 500) // Cứ 0.5 giây quét 1 lần
    @Transactional
    public void autoUpdateAuctionStatus() {
        LocalDateTime now = LocalDateTime.now();
        List<Auction> allAuctions = auctionRepository.findAll();

        for (Auction auction : allAuctions) {
            // Đến giờ bắt đầu -> Đổi thành RUNNING
            if (auction.getStatus() == AuctionStatus.OPEN && now.isAfter(auction.getStartTime())) {
                log.info("SYSTEM CRONJOB: Đã đến giờ mở cửa phiên đấu giá [{}]. Đổi trạng thái sang RUNNING.", auction.getId());
                auction.setStatus(AuctionStatus.RUNNING);
                auctionRepository.save(auction);
            }
            // Đến giờ kết thúc -> Đổi thành FINISHED (Và gửi thông báo nếu muốn)
            if (auction.getStatus() == AuctionStatus.RUNNING && now.isAfter(auction.getEndTime())) {
                log.info("SYSTEM CRONJOB: Đã hết giờ phiên đấu giá [{}]. Tiến hành ĐÓNG phiên...", auction.getId());
                closeAuction(auction.getId());
            }
        }
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