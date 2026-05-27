package org.auctionfx.auctionbidsystemspringbootrework.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
    @Autowired private ApplicationContext applicationContext;
    @Autowired private SimpMessagingTemplate messagingTemplate;

    // Cấu hình thuật toán Anti-Snipping
    private static final int SNIPING_THRESHOLD_SECONDS = 10; // Đấu giá trong 10s cuối
    private static final int EXTENSION_SECONDS = 60; // Thì gia hạn thêm 60s

    // ========================================================================
    // TÍNH TOÁN BƯỚC GIÁ ĐỘNG BẰNG TOÁN HỌC (QUY TẮC 1-2-5)
    // Ngăn chặn bot sinh ra hàng chục nghìn transaction rác cho tài sản lớn
    // ========================================================================
    private BigDecimal calculateDynamicStep(BigDecimal startPrice) {
        if (startPrice == null || startPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal("10000"); // Tối thiểu 10k
        }

        double price = startPrice.doubleValue();

        // 1. Tính bước giá nháp = 2% giá trị sản phẩm
        double rawStep = price * 0.02;

        // Quy định mức thấp nhất không bao giờ dưới 10,000 VND
        if (rawStep < 10000) {
            return new BigDecimal("10000");
        }

        // 2. Tìm bậc (Magnitude) của số đó bằng Log10
        double magnitude = Math.pow(10, Math.floor(Math.log10(rawStep)));

        // 3. Chuẩn hóa về số có 1 chữ số (Từ 1.0 đến 9.999)
        double normalized = rawStep / magnitude;

        // 4. Ép tròn vào các mốc số Đẹp (1, 2, 5, 10)
        double niceDigit;
        if (normalized < 1.5) {
            niceDigit = 1.0;  // Gần 1 -> Làm tròn thành 1
        } else if (normalized < 3.5) {
            niceDigit = 2.0;  // Gần 2 -> Làm tròn thành 2
        } else if (normalized < 7.5) {
            niceDigit = 5.0;  // Gần 5 -> Làm tròn thành 5
        } else {
            niceDigit = 10.0; // Gần 10 -> Kéo lên bậc tiếp theo
        }

        // 5. Nhân ngược lại để ra bước giá thực tế cực tròn trịa
        long finalStep = (long) (niceDigit * magnitude);

        return BigDecimal.valueOf(finalStep);
    }

    // 1. Cài đặt AutoBid
    @Transactional(rollbackFor = Exception.class)
    public String setupAutoBid(String auctionId, String bidderUserName, BigDecimal maxAmount) {
        log.info("SERVICE: Bắt đầu xử lý luồng thêm/cập nhật Autobid. AuctionID={}, Username={}, Amount={}", auctionId, bidderUserName, maxAmount);

        // 1. Tìm phiên đấu giá
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> {
                    log.error("Lỗi: Không tìm thấy phiên đấu giá [{}]", auctionId);
                    return new AuctionException(ErrorCode.AUCTION_NOT_FOUND);
                });

        // 2. TÌM VÀ KIỂM TRA NGƯỜI DÙNG
        User user = userRepository.findByUserName(bidderUserName);
        if (user == null || !(user instanceof Bidder)) {
            log.error("Lỗi: Người dùng [{}] không tồn tại hoặc không phải là Bidder", bidderUserName);
            throw new AuctionException(ErrorCode.USER_INVALID);
        }
        Bidder bidder = (Bidder) user;

        // Kiểm tra Seller
        if (bidder.getId().equals(auction.getSeller().getId())) {
            log.warn("Cảnh báo: Seller [{}] cố tình cài đặt Bot tự đấu giá sản phẩm của chính mình!", bidder.getUserName());
            throw new AuctionException(ErrorCode.AUCTION_BIDDER_INVALID);
        }

        // Kiểm tra Banned
        if (bidder.isBanned()) {
            log.warn("Cảnh báo: Tài khoản [{}] đã bị cấm nhưng vẫn cố cài Autobid", bidderUserName);
            throw new UserException(ErrorCode.USER_BANNED);
        }

        // 3. Kiểm tra maxAmount có lớn hơn giá hiện tại không
        if (maxAmount.compareTo(auction.getHighestBid()) <= 0) {
            log.warn("Cảnh báo: Giá nhập ({}) nhỏ hơn hoặc bằng giá hiện tại ({})", maxAmount, auction.getHighestBid());
            throw new RuntimeException("Mức giá Auto-bid phải cao hơn giá hiện tại (" + String.format("%,d", auction.getHighestBid().longValue()).replace(",", ".") + " VND)");
        }

        if (maxAmount.compareTo(bidder.getMoneyOnWallet()) > 0) {
            log.warn("Cảnh báo: User [{}] cài AutoBid ({}) cao hơn số dư ví hiện tại ({})", bidderUserName, maxAmount, bidder.getMoneyOnWallet());
            throw new RuntimeException("Số tiền cài đặt Auto-bid không được vượt quá số dư khả dụng trong ví của bạn!");
        }

        // 4. KIỂM TRA MỨC GIÁ AUTO-BID CÓ PHẢI LÀ BỘI SỐ CỦA BƯỚC GIÁ HAY KHÔNG?
        BigDecimal stepPrice = auction.getStepPrice() != null ? auction.getStepPrice() : calculateDynamicStep(auction.getBidProduct().getStartPrice());
        if (maxAmount.remainder(stepPrice).compareTo(BigDecimal.ZERO) != 0) {
            String stepStr = String.format("%,d", stepPrice.longValue()).replace(",", ".");
            log.warn("Cảnh báo: Giá Auto-bid ({}) không phải là bội số của bước giá ({})", maxAmount, stepPrice);
            throw new RuntimeException("Tiền cài đặt Auto-bid phải là bội số của " + stepStr + " VND để hệ thống dễ khớp giá.");
        }

        // ========================================================================
        // [ĐÃ SỬA] 5. TÌM BOT CŨ VÀ TỰ ĐỘNG DỌN RÁC DATABASE
        // ========================================================================
        List<AutoBidConfig> existingConfigs = autoBidConfigRepository.findByAuctionAndBidder(auction, bidder);
        AutoBidConfig config;

        if (!existingConfigs.isEmpty()) {
            // Lấy cái bot đầu tiên ra để dùng (tái chế)
            config = existingConfigs.get(0);

            // TÍNH NĂNG TỰ CHỮA LÀNH: Xóa hết các bot thừa thãi do lỗi code cũ tạo ra
            if (existingConfigs.size() > 1) {
                for (int i = 1; i < existingConfigs.size(); i++) {
                    autoBidConfigRepository.delete(existingConfigs.get(i));
                }
                log.info("DỌN RÁC DB: Đã tự động xóa {} bản ghi Bot trùng lặp của user [{}]",
                        (existingConfigs.size() - 1), bidder.getUserName());
            }
        } else {
            // Nếu rỗng thật thì mới tạo cái mới
            config = new AutoBidConfig();
        }

        config.setAuction(auction);
        config.setBidder(bidder);
        config.setMaxBidAmount(maxAmount); // Cập nhật mức tiền mới

        // ĐÁNH THỨC BOT DẬY
        config.setActive(true);

        autoBidConfigRepository.save(config);

        // KÍCH HOẠT AUTOBID NGAY LẬP TỨC
        triggerAutoBidProcess(auction);

        return "Setup/Update Auto-bid successfully!";
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
        // 1. Lấy thông tin người dẫn đầu cũ và giá cũ
        Bidder previousWinner = auction.getWinningUser();
        BigDecimal previousBid = auction.getHighestBid();

        // 2. XỬ LÝ ĐÓNG BĂNG / RÃ ĐÔNG TIỀN (CHUẨN LOGIC TÀI CHÍNH)
        if (previousWinner != null && previousWinner.getId().equals(bidder.getId())) {
            // TRƯỜNG HỢP 1: TỰ ĐÈ GIÁ CHÍNH MÌNH (Self-outbid)
            // Chỉ đóng băng thêm phần tiền chênh lệch để tránh lỗi "Thiếu số dư khả dụng ảo"
            BigDecimal extraAmount = bidAmount.subtract(previousBid);
            log.info("Bidder [{}] tự nâng giá. Đóng băng thêm phần chênh lệch: {} VND", bidder.getUserName(), extraAmount);
            paymentService.freezeMoney(bidder.getId(), extraAmount);
        } else {
            // TRƯỜNG HỢP 2: NGƯỜI MỚI VƯỢT GIÁ NGƯỜI CŨ
            // Rã đông trả lại toàn bộ tiền cho người cũ (nếu có)
            if (previousWinner != null) {
                log.info("Thực hiện hoàn trả tiền {} VND cho người dẫn đầu cũ [{}]", previousBid, previousWinner.getUserName());
                paymentService.unFreezeMoney(previousWinner.getId(), previousBid);
            }
            // Đóng băng toàn bộ số tiền của người mới
            log.info("Thực hiện đóng băng {} VND của Bidder mới [{}]", bidAmount, bidder.getUserName());
            paymentService.freezeMoney(bidder.getId(), bidAmount);
        }

        // 3. Cập nhật thông tin phiên đấu giá
        auction.setHighestBid(bidAmount);
        auction.setWinningUser(bidder);

        // 4. Thuật toán ANTI-SNIPPING (Chống bắn tỉa giây cuối)
        boolean isExtended = false;
        long secondsRemaining = ChronoUnit.SECONDS.between(LocalDateTime.now(), auction.getEndTime());
        if (secondsRemaining <= SNIPING_THRESHOLD_SECONDS) {
            log.info("HỆ THỐNG ANTI-SNIPPING KÍCH HOẠT: Chỉ còn {} giây. Gia hạn thêm {} giây cho phiên [{}]", secondsRemaining, EXTENSION_SECONDS, auction.getId());
            auction.setEndTime(auction.getEndTime().plusSeconds(EXTENSION_SECONDS));
            isExtended = true;
        }

        // 5. Lưu lịch sử giao dịch
        BidTransaction transaction = new BidTransaction();
        transaction.setAuction(auction);
        transaction.setBidder(bidder);
        transaction.setBidAmount(bidAmount);
        transaction.setBidTimestamp(LocalDateTime.now());

        bidTransactionRepository.save(transaction);
        auctionRepository.save(auction);

        // =========================================================
        // WEBSOCKET: BẮN TÍN HIỆU CÓ GIÁ MỚI CHO TẤT CẢ AI ĐANG XEM
        // =========================================================
        try {
            messagingTemplate.convertAndSend("/topic/auctions/" + auction.getId(), "NEW_BID");
            messagingTemplate.convertAndSend("/topic/auctions/global", "UPDATE");
            log.info("📢 WEBSOCKET: Đã phát tín hiệu có giá mới cho phiên [{}]", auction.getId());
        } catch (Exception e) {
            log.error("Lỗi gửi WebSocket: ", e);
        }

        return isExtended;
    }

    // Kích hoạt AutoBid
    private void triggerAutoBidProcess(Auction auction) {
        boolean keepBidding = true;

        // TẠO CÁI "GIỎ" ĐỂ LƯU TẠM THÔNG BÁO VÀ NGĂN SPAM
        Map<String, BigDecimal> botHighestBids = new HashMap<>();
        Map<String, Bidder> botUsers = new HashMap<>();

        while (keepBidding) {
            keepBidding = false;

            List<AutoBidConfig> autoBids = autoBidConfigRepository.findByAuctionAndIsActiveTrueOrderByCreatedAtAsc(auction);
            BigDecimal stepPrice = auction.getStepPrice() != null ? auction.getStepPrice() : calculateDynamicStep(auction.getBidProduct().getStartPrice());
            String shortId = auction.getBidProduct().getId().substring(0, 4).toUpperCase();

            for (AutoBidConfig autoBid : autoBids) {
                if (auction.getWinningUser() != null && auction.getWinningUser().getId().equals(autoBid.getBidder().getId())) {
                    continue;
                }

                BigDecimal nextBidAmount = auction.getHighestBid().add(stepPrice);

                if (nextBidAmount.compareTo(autoBid.getMaxBidAmount()) <= 0) {
                    User freshUser = userRepository.findById(autoBid.getBidder().getId()).orElse(null);
                    if (freshUser instanceof Bidder) {
                        Bidder freshBidder = (Bidder) freshUser;

                        if (freshBidder.getMoneyOnWallet().compareTo(nextBidAmount) < 0) {
                            log.warn("AUTOBID STOP: Bot của [{}] bị tắt do thiếu tiền.", freshBidder.getUserName());
                            autoBid.setActive(false);
                            autoBidConfigRepository.save(autoBid);

                            // Thông báo HẾT TIỀN (Chỉ bắn 1 lần khi tắt bot)
                            Notification notif = new Notification();
                            notif.setUser(freshBidder);
                            notif.setAuction(auction);
                            notif.setType(NotificationType.AUCTION_FAILED);
                            notif.setTitle("AutoBid TẮT: Thiếu số dư (SP" + shortId + ")");
                            notif.setDescription("Bot đã dừng. Lý do: Ví chỉ còn " + String.format("%,d", freshBidder.getMoneyOnWallet().longValue()).replace(",", ".") + " VND, không đủ để trả " + String.format("%,d", nextBidAmount.longValue()).replace(",", ".") + " VND.");
                            notificationRepository.save(notif);

                            continue;
                        }
                    }

                    log.info("AUTOBID: Kích hoạt bot của [{}] đánh giá [{}].", autoBid.getBidder().getUserName(), nextBidAmount);
                    executeInternalBid(auction, autoBid.getBidder(), nextBidAmount);

                    // THAY VÌ BẮN THÔNG BÁO NGAY -> LƯU TẠM VÀO GIỎ
                    // Nếu chạy qua chạy lại 100 vòng, nó chỉ ghi đè mức giá mới nhất, không sinh ra 100 thông báo rác!
                    botHighestBids.put(autoBid.getBidder().getId(), nextBidAmount);
                    botUsers.put(autoBid.getBidder().getId(), autoBid.getBidder());

                    keepBidding = true;
                    break;
                } else {
                    log.info("AUTOBID: Bot của [{}] đã đuối sức (Max: {}). Tắt bot.", autoBid.getBidder().getUserName(), autoBid.getMaxBidAmount());
                    autoBid.setActive(false);
                    autoBidConfigRepository.save(autoBid);

                    // Thông báo QUÁ MỨC TRẦN (Chỉ bắn 1 lần khi tắt bot)
                    Notification maxNotif = new Notification();
                    maxNotif.setUser(autoBid.getBidder());
                    maxNotif.setAuction(auction);
                    maxNotif.setType(NotificationType.AUCTION_FAILED);
                    maxNotif.setTitle("AutoBid TẮT: Vượt mức trần (SP" + shortId + ")");
                    maxNotif.setDescription("Bot đã dừng. Lý do: Mức giá cần đánh (" + String.format("%,d", nextBidAmount.longValue()).replace(",", ".") + " VND) cao hơn mức giá MAX (" + String.format("%,d", autoBid.getMaxBidAmount().longValue()).replace(",", ".") + " VND).");
                    notificationRepository.save(maxNotif);
                }
            }
        }

        // =================================================================================
        // TỔNG KẾT SAU KHI KẾT THÚC VÒNG LẶP (BOT ĐÃ ĐÁNH NHAU XONG)
        // =================================================================================
        String finalShortId = auction.getBidProduct().getId().substring(0, 4).toUpperCase();
        for (String userId : botHighestBids.keySet()) {
            Bidder bidder = botUsers.get(userId);
            BigDecimal finalAmount = botHighestBids.get(userId);

            Notification successNotif = new Notification();
            successNotif.setUser(bidder);
            successNotif.setAuction(auction);
            successNotif.setType(NotificationType.AUCTION_SUCCESS);

            String priceFormatted = String.format("%,d", finalAmount.longValue()).replace(",", ".");

            // Phân loại nội dung thông báo thông minh:
            if (auction.getWinningUser() != null && auction.getWinningUser().getId().equals(bidder.getId())) {
                // Nếu Bot này là kẻ chiến thắng cuối cùng
                successNotif.setTitle("AutoBid: Dẫn đầu SP" + finalShortId);
                successNotif.setDescription("Bot đã tự động đấu giá và bạn đang DẪN ĐẦU với mức giá " + priceFormatted + " VND.");
            } else {
                // Nếu Bot này có tham gia đánh, nhưng cuối cùng vẫn thua (Bị ngắt do hết tiền hoặc chạm trần)
                successNotif.setTitle("AutoBid: Vừa hoạt động SP" + finalShortId);
                successNotif.setDescription("Trong đợt cạnh tranh vừa rồi, Bot của bạn đã đấu giá lên tới mức " + priceFormatted + " VND.");
            }

            notificationRepository.save(successNotif);
        }
    }

    // 3. Bắt đầu và kết thúc phiên đấu giá
    @Transactional
    public void startAuction(String auctionId) {
        log.info("Bắt đầu khởi chạy thủ công phiên đấu giá[{}]", auctionId);
        Auction auction = auctionRepository.findById(auctionId).orElseThrow();
        auction.setStatus(AuctionStatus.RUNNING);
        auctionRepository.save(auction);

        // THÊM 3 DÒNG NÀY ĐỂ BÁO JAVAFX MỞ KHÓA NÚT BẤM
        try {
            messagingTemplate.convertAndSend("/topic/auctions/" + auctionId, "STATUS_UPDATE");
            // Bắn tín hiệu Toàn cầu cho màn hình Danh sách (AuctionList) biết để cập nhật ngầm
            messagingTemplate.convertAndSend("/topic/auctions/global", "UPDATE");
        } catch (Exception e) { log.error("Lỗi WS: ", e); }
    }

    @Transactional
    public String closeAuction(String auctionId) {
        log.info("Bắt đầu quá trình ĐÓNG phiên đấu giá [{}]", auctionId);
        Auction auction = auctionRepository.findById(auctionId).orElseThrow();
        auction.setStatus(AuctionStatus.FINISHED);
        String shortId = auction.getBidProduct().getId().substring(0, 4).toUpperCase();

        String resultMessage;

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

            // 2. Thông báo cho Người bán (Biết đã có người thắng)
            Notification sellerNotif = new Notification();
            sellerNotif.setUser(auction.getSeller());
            sellerNotif.setAuction(auction);
            sellerNotif.setType(NotificationType.AUCTION_SUCCESS);
            sellerNotif.setTitle("Phiên đấu giá kết thúc: SP" + shortId);
            sellerNotif.setDescription("Người thắng: " + auction.getWinningUser().getFullName() + " với giá " + auction.getHighestBid() + " VND. Đang chờ người mua xác thực thanh toán.");
            notificationRepository.save(sellerNotif);

            resultMessage = "Session ended! Winner is: " + auction.getWinningUser().getFullName();
        } else {
            log.info("Phiên đấu giá [{}] kết thúc nhưng không ai đấu giá", auctionId);
            auction.setTransactionStatus(TransactionStatus.FAILED);

            // 3. Thông báo cho Người bán (Báo ế)
            Notification sellerFailNotif = new Notification();
            sellerFailNotif.setUser(auction.getSeller());
            sellerFailNotif.setAuction(auction);
            sellerFailNotif.setType(NotificationType.AUCTION_FAILED);
            sellerFailNotif.setTitle("Phiên đấu giá kết thúc: SP" + shortId);
            sellerFailNotif.setDescription("Sản phẩm " + auction.getBidProduct().getName() + " đã hết thời gian nhưng không có ai tham gia trả giá.");
            notificationRepository.save(sellerFailNotif);

            resultMessage = "Session ended! No one winner";
        }

        // Lưu thông tin vào DB một lần duy nhất cho cả 2 nhánh
        auctionRepository.save(auction);

        // ========================================================
        // BẮN WEBSOCKET BÁO CHO JAVAFX KHÓA NÚT NGAY LẬP TỨC
        // ========================================================
        try {
            messagingTemplate.convertAndSend("/topic/auctions/" + auctionId, "STATUS_UPDATE");
            // Bắn tín hiệu Toàn cầu cho màn hình Danh sách (AuctionList) biết để cập nhật ngầm
            messagingTemplate.convertAndSend("/topic/auctions/global", "UPDATE");
        } catch (Exception e) {
            log.error("Lỗi WS khi đóng phiên đấu giá: ", e);
        }

        return resultMessage;
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
        auction.setTransactionStatus(TransactionStatus.CANCELLED);

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
        // Lấy thời gian bắt đầu từ Request thay vì now()
        auction.setStartTime(request.getStartTime());
        auction.setEndTime(request.getEndTime());
        auction.setHighestBid(item.getStartPrice()); // Giá khởi điểm

        auctionRepository.save(auction);
        log.info("Tạo phiên đấu giá thành công. AuctionID={}", auction.getId());
        return "Create Auction item successfully! ID: " + auction.getId();
    }

    // Lấy danh sách tất cả các phiên đấu giá
    public List<Auction> getAllAuctions(String username) {
        List<Auction> auctions = auctionRepository.findAll();

        // Nếu có tên người dùng gửi lên, ta lướt qua xem họ có đặt giá không
        if (username != null && !username.trim().isEmpty()) {
            for (Auction auction : auctions) {
                BigDecimal myMax = BigDecimal.ZERO;

                // Tránh lỗi NullPointer nếu phiên chưa có ai đấu giá
                if (auction.getBidTransactions() != null) {
                    for (BidTransaction tx : auction.getBidTransactions()) {
                        if (tx.getBidder() != null && username.equals(tx.getBidder().getUserName())) {
                            if (tx.getBidAmount().compareTo(myMax) > 0) {
                                myMax = tx.getBidAmount();
                            }
                        }
                    }
                }
                // Gắn con số vừa tìm được vào biến ảo để gửi về cho JavaFX
                auction.setMyHighestBid(myMax);
            }
        }
        return auctions;
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

    // Lấy thông tin mức giá Auto-bid đang chạy của User (Nếu có)
    public BigDecimal getMyAutoBid(String auctionId, String username) {
        Auction auction = auctionRepository.findById(auctionId).orElse(null);
        if (auction == null) return null;

        // Quét danh sách Bot đang chạy của phiên này
        List<AutoBidConfig> autoBids = autoBidConfigRepository.findByAuctionAndIsActiveTrueOrderByCreatedAtAsc(auction);
        for (AutoBidConfig config : autoBids) {
            // Nếu thấy tên user trùng khớp, trả về số tiền
            if (config.getBidder().getUserName().equals(username)) {
                return config.getMaxBidAmount();
            }
        }
        return null; // Không có thì trả về null
    }

    public List<Auction> getMyAuctions(String username) {
        return auctionRepository.findBySellerUserName(username);
    }

    // Lấy thông tin chi tiết của 1 phiên đấu giá
    public Auction getAuctionById(String auctionId) {
        return auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionException(ErrorCode.AUCTION_NOT_FOUND));
    }

    // Tự động quét Auction
    @Scheduled(fixedRate = 2000)
    public void autoUpdateAuctionStatus() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        List<Auction> allAuctions = auctionRepository.findAll();

        log.info("=== BẮT ĐẦU QUÉT {} PHIÊN | NOW (VN Time): {} ===", allAuctions.size(), now);

        for (Auction auction : allAuctions) {
            try {
                // Lấy Proxy chuẩn của AuctionService để giữ nguyên @Transactional
                AuctionService proxyService = applicationContext.getBean(AuctionService.class);

                // Đổi thành RUNNING
                if (auction.getStatus() == AuctionStatus.OPEN && now.isAfter(auction.getStartTime())) {
                    log.info(">> Đã đến giờ, đổi sang RUNNING...");
                    proxyService.startAuction(auction.getId());
                }

                // Đổi thành FINISHED
                if (auction.getStatus() == AuctionStatus.RUNNING && (now.isAfter(auction.getEndTime()) || now.isEqual(auction.getEndTime()))) {
                    log.info(">> Đã hết giờ, tiến hành ĐÓNG phiên...");
                    proxyService.closeAuction(auction.getId());
                }
            } catch (Exception e) {
                log.error("❌ CRONJOB LỖI ở phiên [{}]: {}", auction.getId(), e.getMessage());
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