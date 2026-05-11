package org.auctionfx.auctionbidsystemspringbootrework.service;

import lombok.extern.slf4j.Slf4j;
import org.auctionfx.auctionbidsystemspringbootrework.dto.response.NotificationResponse;
import org.auctionfx.auctionbidsystemspringbootrework.entity.auction.Auction;
import org.auctionfx.auctionbidsystemspringbootrework.entity.notification.Notification;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.User;
import org.auctionfx.auctionbidsystemspringbootrework.enums.NotificationType;
import org.auctionfx.auctionbidsystemspringbootrework.exception.ErrorCode;
import org.auctionfx.auctionbidsystemspringbootrework.exception.NotificationException;
import org.auctionfx.auctionbidsystemspringbootrework.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AuctionService auctionService;

    // 1. Lấy danh sách thông báo cho JavaFX
    public List<NotificationResponse> getMyNotifications(String userName) {
        log.info("Bắt đầu truy xuất danh sách thông báo từ DB cho user: {}", userName);
        List<Notification> rawList = notificationRepository.findByUserUserNameOrderByCreatedAtDesc(userName);
        List<NotificationResponse> responseList = new ArrayList<>();

        for (Notification notif : rawList) {
            responseList.add(new NotificationResponse(
                    notif.getId(),
                    notif.getTitle(),
                    notif.getDescription(),
                    notif.getType(),
                    notif.isRead(),
                    notif.getCreatedAt()
            ));
        }
        log.info("Truy xuất thành công {} thông báo cho user: {}", responseList.size(), userName);
        return responseList;
    }

    // 2. Xoá thông báo (nút thùng rác)
    @Transactional
    public String deleteNotification(String notificationId) {
        log.info("Thực hiện xóa thông báo ID: {}", notificationId);
        Notification notif = notificationRepository.findById(notificationId)
                .orElseThrow(() -> {
                    log.error("Không tìm thấy thông báo ID: {} để xóa", notificationId);
                    return new NotificationException(ErrorCode.NOTIFICATION_NOT_FOUND);
                });

        if (notif.getType() == NotificationType.PAYMENT_VERIFICATION) {
            log.warn("Không thể xóa thông báo ID: {} vì thuộc loại PAYMENT_VERIFICATION", notificationId);
            throw new NotificationException(ErrorCode.NOTIFICATION_DELETE_INVALID);
        }

        notificationRepository.delete(notif);
        log.info("Xóa thành công thông báo ID: {}", notificationId);
        return "Delete notification successfully!";
    }

    // 3. Xử lý nút tích xanh (Chấp nhận trả tiền)
    @Transactional
    public String acceptPayment(String notificationId) {
        log.info("Xử lý chấp nhận thanh toán cho thông báo ID: {}", notificationId);
        Notification oldNotif = notificationRepository.findById(notificationId)
                .orElseThrow(() -> {
                    log.error("Không tìm thấy thông báo ID: {} để chấp nhận thanh toán", notificationId);
                    return new NotificationException(ErrorCode.NOTIFICATION_NOT_FOUND);
                });

        if (oldNotif.getType() != NotificationType.PAYMENT_VERIFICATION) {
            log.warn("Thông báo ID: {} không phải loại PAYMENT_VERIFICATION. Rejecting request.", notificationId);
            throw new NotificationException(ErrorCode.NOTIFICATION_ACCEPT_PAYMENT_INVALID);
        }

        auctionService.acceptPayment(oldNotif.getAuction().getId());
        String shortId = oldNotif.getAuction().getBidProduct().getId().substring(0, 4).toUpperCase();

        // 3.1 Tạo thông báo cho NGƯỜI MUA (Đã thanh toán)
        Notification buyerSuccessNotif = new Notification();
        buyerSuccessNotif.setUser(oldNotif.getUser());
        buyerSuccessNotif.setAuction(oldNotif.getAuction());
        buyerSuccessNotif.setType(NotificationType.AUCTION_SUCCESS);
        buyerSuccessNotif.setTitle("Thanh toán thành công: SP" + shortId);
        buyerSuccessNotif.setDescription("Bạn đã thanh toán thành công số tiền: " + oldNotif.getAuction().getHighestBid() + " VND");
        notificationRepository.save(buyerSuccessNotif);

        // 3.2 Tạo thông báo cho NGƯỜI BÁN (Nhận được tiền)
        Notification sellerSuccessNotif = new Notification();
        sellerSuccessNotif.setUser(oldNotif.getAuction().getSeller());
        sellerSuccessNotif.setAuction(oldNotif.getAuction());
        sellerSuccessNotif.setType(NotificationType.AUCTION_SUCCESS);
        sellerSuccessNotif.setTitle("Tiền đã vào ví: SP" + shortId);
        sellerSuccessNotif.setDescription("Người mua " + oldNotif.getUser().getFullName() + " đã thanh toán " + oldNotif.getAuction().getHighestBid() + " VND cho sản phẩm " + oldNotif.getAuction().getBidProduct().getName());
        notificationRepository.save(sellerSuccessNotif);

        // Xóa thông báo xác thực cũ đi
        notificationRepository.delete(oldNotif);

        log.info("Giao dịch thành công cho Auction ID: {}. Đã gửi thông báo đến Buyer và Seller.", oldNotif.getAuction().getId());
        return "Accept payment successfully!";
    }

    // 4. Xử lý nút chữ X đỏ (Từ chối thanh toán)
    @Transactional
    public String declinePayment(String notificationId) {
        log.info("Xử lý từ chối thanh toán cho thông báo ID: {}", notificationId);
        Notification oldNotif = notificationRepository.findById(notificationId)
                .orElseThrow(() -> {
                    log.error("Không tìm thấy thông báo ID: {} để từ chối thanh toán", notificationId);
                    return new NotificationException(ErrorCode.NOTIFICATION_NOT_FOUND);
                });

        if (oldNotif.getType() != NotificationType.PAYMENT_VERIFICATION) {
            log.warn("Thông báo ID: {} không phải loại PAYMENT_VERIFICATION. Rejecting request.", notificationId);
            throw new NotificationException(ErrorCode.NOTIFICATION_DECLINE_PAYMENT_INVALID);
        }

        auctionService.declinePayment(oldNotif.getAuction().getId());
        String shortId = oldNotif.getAuction().getBidProduct().getId().substring(0, 4).toUpperCase();

        // 4.1 Tạo thông báo cho NGƯỜI MUA (Hủy thanh toán)
        Notification buyerFailedNotif = new Notification();
        buyerFailedNotif.setUser(oldNotif.getUser());
        buyerFailedNotif.setAuction(oldNotif.getAuction());
        buyerFailedNotif.setType(NotificationType.AUCTION_FAILED);
        buyerFailedNotif.setTitle("Đã hủy thanh toán: SP" + shortId);
        buyerFailedNotif.setDescription("Bạn đã từ chối thanh toán. Tiền đóng băng đã được hoàn trả về ví chính.");
        notificationRepository.save(buyerFailedNotif);

        // 4.2 Tạo thông báo cho NGƯỜI BÁN (Bị boom hàng)
        Notification sellerFailedNotif = new Notification();
        sellerFailedNotif.setUser(oldNotif.getAuction().getSeller());
        sellerFailedNotif.setAuction(oldNotif.getAuction());
        sellerFailedNotif.setType(NotificationType.AUCTION_FAILED);
        sellerFailedNotif.setTitle("Giao dịch bị hủy: SP" + shortId);
        sellerFailedNotif.setDescription("Người mua " + oldNotif.getUser().getFullName() + " đã từ chối thanh toán cho sản phẩm " + oldNotif.getAuction().getBidProduct().getName() + ". Giao dịch thất bại.");
        notificationRepository.save(sellerFailedNotif);

        // Xóa thông báo xác thực cũ đi
        notificationRepository.delete(oldNotif);

        log.info("Hủy giao dịch cho Auction ID: {}. Đã gửi thông báo đến Buyer và Seller.", oldNotif.getAuction().getId());
        return "Decline payment successfully!";
    }

    // Tạo notification mới <-> nguyên tắc DRY
    @Transactional
    public Notification createNotification(User user, Auction auction, NotificationType type, String title, String description) {
        log.debug("Tạo mới thông báo - User: {}, Type: {}, Title: {}", user.getUserName(), type, title);
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setAuction(auction);
        notification.setType(type);
        notification.setTitle(title);
        notification.setDescription(description);
        notification.setRead(false);

        Notification savedNotification = notificationRepository.save(notification);
        log.debug("Đã lưu thành công thông báo ID: {}", savedNotification.getId());
        return savedNotification;
    }
}