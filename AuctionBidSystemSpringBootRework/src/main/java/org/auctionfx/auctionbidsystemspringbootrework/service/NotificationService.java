package org.auctionfx.auctionbidsystemspringbootrework.service;

import org.auctionfx.auctionbidsystemspringbootrework.dto.response.NotificationResponse;
import org.auctionfx.auctionbidsystemspringbootrework.entity.notification.Notification;
import org.auctionfx.auctionbidsystemspringbootrework.enums.NotificationType;
import org.auctionfx.auctionbidsystemspringbootrework.exception.ErrorCode;
import org.auctionfx.auctionbidsystemspringbootrework.exception.NotificationException;
import org.auctionfx.auctionbidsystemspringbootrework.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationService {
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private AuctionService auctionService;

    // 1. Lấy danh sách thông báo cho JavaFX
    public List<NotificationResponse> getMyNotifications(String userName) {
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
        return responseList;
    }

    // 2. Xoá thông báo (nút thùng rác)
    @Transactional
    public String deleteNotification(String notificationId) {
        Notification notif = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (notif.getType() == NotificationType.PAYMENT_VERIFICATION) {
            throw new NotificationException(ErrorCode.NOTIFICATION_DELETE_INVALID);
        }

        notificationRepository.delete(notif);
        return "Delete notification successfully!";
    }

    // 3. Xử lý nút tích xanh (Chấp nhận trả tiền)
    @Transactional
    public String acceptPayment(String notificationId) {
        Notification oldNotif = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (oldNotif.getType() != NotificationType.PAYMENT_VERIFICATION) {
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

        return "Accept payment successfully!";
    }

    // 4. Xử lý nút chữ X đỏ (Từ chối thanh toán)
    @Transactional
    public String declinePayment(String notificationId) {
        Notification oldNotif = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (oldNotif.getType() != NotificationType.PAYMENT_VERIFICATION) {
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

        return "Decline payment successfully!";
    }
}