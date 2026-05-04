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

        // Bảo mật: Chặn không cho xóa thông báo xác thực
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

        // Gọi logic chuyển tiền (Trừ ví đóng băng của người mua, cộng ví người bán)
        auctionService.acceptPayment(oldNotif.getAuction().getId());

        // Bước 1: Tạo thông báo mới (thành công)
        Notification successNotif = new Notification();
        successNotif.setUser(oldNotif.getUser());
        successNotif.setAuction(oldNotif.getAuction());
        successNotif.setType(NotificationType.AUCTION_SUCCESS);
        successNotif.setTitle("Sản phẩm đấu giá thành công: SP " +
                oldNotif.getAuction().getBidProduct().getName().toUpperCase());
        successNotif.setDescription("Bạn đã thanh toán thành công số tiền: " +
                oldNotif.getAuction().getHighestBid() + " VND");
        successNotif.setRead(false);
        notificationRepository.save(successNotif);

        // Bước 2: Xóa thông báo xác thực cũ đi
        notificationRepository.delete(oldNotif);

        return "Accept payment successfully!";
    }

    // 4. Xử lý nút chữ X đỏ (Hoàn trả tiền đóng băng cho người mua)
    @Transactional
    public String declinePayment(String notificationId) {
        Notification oldNotif = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (oldNotif.getType() != NotificationType.PAYMENT_VERIFICATION) {
            throw new NotificationException(ErrorCode.NOTIFICATION_DECLINE_PAYMENT_INVALID);
        }

        // Gọi logic Hủy phiên (Hoàn trả tiền đóng băng cho người mua)
        auctionService.declinePayment(oldNotif.getAuction().getId());

        // Bước 1: Tạo thông báo mới (thất bại)
        Notification failedNotif = new Notification();
        failedNotif.setUser(oldNotif.getUser());
        failedNotif.setAuction(oldNotif.getAuction());
        failedNotif.setType(NotificationType.AUCTION_FAILED);
        failedNotif.setTitle("Đã hủy thanh toán giao dịch: SP " +
                oldNotif.getAuction().getBidProduct().getName().toUpperCase());
        failedNotif.setDescription("Bạn đã từ chối thanh toán. Tiền đóng băng đã được hoàn trả về ví chính.");
        failedNotif.setRead(false);
        notificationRepository.save(failedNotif);

        // Bước 2: Xóa thông báo xác thực cũ đi
        notificationRepository.delete(oldNotif);

        return "Decline payment successfully!";
    }
}
