package org.auctionfx.auctionbidsystemspringbootrework.service;

import lombok.extern.slf4j.Slf4j;
import org.auctionfx.auctionbidsystemspringbootrework.dto.response.NotificationResponse;
import org.auctionfx.auctionbidsystemspringbootrework.entity.auction.Auction;
import org.auctionfx.auctionbidsystemspringbootrework.entity.notification.Notification;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.User;
import org.auctionfx.auctionbidsystemspringbootrework.enums.NotificationType;
import org.auctionfx.auctionbidsystemspringbootrework.enums.Role;
import org.auctionfx.auctionbidsystemspringbootrework.exception.ErrorCode;
import org.auctionfx.auctionbidsystemspringbootrework.exception.NotificationException;
import org.auctionfx.auctionbidsystemspringbootrework.repository.NotificationRepository;
import org.auctionfx.auctionbidsystemspringbootrework.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
    private UserRepository userRepository;

    @Autowired
    private AuctionService auctionService;

    @Autowired
    @Lazy
    private UserService userService;

    // Kéo ChatService vào để xử lý kết bạn
    @Autowired
    @Lazy
    private ChatService chatService;

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

        // BỔ SUNG UPGRADE_REQUEST VÀO DANH SÁCH CẤM XÓA NGANG
        if (notif.getType() == NotificationType.PAYMENT_VERIFICATION ||
                notif.getType() == NotificationType.FRIEND_REQUEST ||
                notif.getType() == NotificationType.UPGRADE_REQUEST) {
            log.warn("Không thể xóa thông báo ID: {} vì nó yêu cầu hành động Xác nhận/Từ chối", notificationId);
            throw new NotificationException(ErrorCode.NOTIFICATION_DELETE_INVALID);
        }

        notificationRepository.delete(notif);
        log.info("Xóa thành công thông báo ID: {}", notificationId);
        return "Delete notification successfully!";
    }

    // =========================================================================
    // 3. XỬ LÝ NÚT TÍCH XANH (CHẤP NHẬN ĐA NĂNG)
    // =========================================================================
    @Transactional
    public String acceptNotification(String notificationId) {
        log.info("Xử lý CHẤP NHẬN hành động cho thông báo ID: {}", notificationId);
        Notification oldNotif = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // 3.A: TRƯỜNG HỢP XÁC NHẬN THANH TOÁN
        if (oldNotif.getType() == NotificationType.PAYMENT_VERIFICATION) {
            auctionService.acceptPayment(oldNotif.getAuction().getId());
            String shortId = oldNotif.getAuction().getBidProduct().getId().substring(0, 4).toUpperCase();

            // Tạo thông báo cho NGƯỜI MUA (Đã thanh toán)
            createNotification(oldNotif.getUser(), oldNotif.getAuction(), NotificationType.AUCTION_SUCCESS,
                    "Thanh toán thành công: SP" + shortId,
                    "Bạn đã thanh toán thành công số tiền: " + oldNotif.getAuction().getHighestBid() + " VND");

            // Tạo thông báo cho NGƯỜI BÁN (Nhận được tiền)
            createNotification(oldNotif.getAuction().getSeller(), oldNotif.getAuction(), NotificationType.AUCTION_SUCCESS,
                    "Tiền đã vào ví: SP" + shortId,
                    "Người mua " + oldNotif.getUser().getFullName() + " đã thanh toán " + oldNotif.getAuction().getHighestBid() + " VND cho sản phẩm " + oldNotif.getAuction().getBidProduct().getName());

            notificationRepository.delete(oldNotif);
            return "Accept payment successfully!";
        }

        // 3.B: TRƯỜNG HỢP CHẤP NHẬN KẾT BẠN
        else if (oldNotif.getType() == NotificationType.FRIEND_REQUEST) {
            // Lấy tên người gửi bị giấu trong title (Ví dụ title: "Yêu cầu kết bạn từ: LaziChicken7")
            String senderUsername = oldNotif.getTitle().replace("Yêu cầu kết bạn từ: ", "").trim();
            String receiverUsername = oldNotif.getUser().getUserName();

            // Gọi ChatService xử lý kết bạn trong DB
            chatService.acceptFriendRequest(senderUsername, receiverUsername);

            // Báo lại cho người gửi biết là mình đã đồng ý
            User sender = userRepository.findByUserName(senderUsername);
            if (sender != null) {
                createNotification(sender, null, NotificationType.AUCTION_SUCCESS, // Dùng tạm type SUCCESS để nó hiện thùng rác
                        "Kết bạn thành công",
                        "Người dùng " + receiverUsername + " đã chấp nhận lời mời kết bạn của bạn.");
            }

            notificationRepository.delete(oldNotif);
            return "Accept friend request successfully!";
        }

        // 3.C. DUYỆT LÊN SELLER (MỚI)
        else if (oldNotif.getType() == NotificationType.UPGRADE_REQUEST) {
            String bidderUsername = oldNotif.getTitle().replace("Yêu cầu lên Seller từ: ", "").trim();

            // Gọi hàm up Seller (Hàm này của bạn đã có sẵn code bắn thông báo chúc mừng cho Bidder và các Admin khác rồi)
            userService.upgradeBidderToSeller(bidderUsername);

            // Xóa tất cả các thông báo yêu cầu này trong hòm thư của các Admin khác (Để họ không bấm nhầm nữa)
            List<Notification> relatedNotifs = notificationRepository.findByTitle(oldNotif.getTitle());
            notificationRepository.deleteAll(relatedNotifs);

            return "Đã phê duyệt yêu cầu lên Seller!";
        }

        throw new NotificationException(ErrorCode.NOTIFICATION_ACCEPT_PAYMENT_INVALID);
    }

    // =========================================================================
    // 4. XỬ LÝ NÚT CHỮ X ĐỎ (TỪ CHỐI ĐA NĂNG)
    // =========================================================================
    @Transactional
    public String declineNotification(String notificationId, String reason) {
        log.info("Xử lý TỪ CHỐI hành động cho thông báo ID: {}", notificationId);
        Notification oldNotif = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // 4.A: TỪ CHỐI THANH TOÁN
        if (oldNotif.getType() == NotificationType.PAYMENT_VERIFICATION) {
            auctionService.declinePayment(oldNotif.getAuction().getId());
            String shortId = oldNotif.getAuction().getBidProduct().getId().substring(0, 4).toUpperCase();

            createNotification(oldNotif.getUser(), oldNotif.getAuction(), NotificationType.AUCTION_FAILED,
                    "Đã hủy thanh toán: SP" + shortId,
                    "Bạn đã từ chối thanh toán. Tiền đóng băng đã được hoàn trả về ví chính.");

            createNotification(oldNotif.getAuction().getSeller(), oldNotif.getAuction(), NotificationType.AUCTION_FAILED,
                    "Giao dịch bị hủy: SP" + shortId,
                    "Người mua " + oldNotif.getUser().getFullName() + " đã từ chối thanh toán cho sản phẩm " + oldNotif.getAuction().getBidProduct().getName() + ". Giao dịch thất bại.");

            notificationRepository.delete(oldNotif);
            return "Decline payment successfully!";
        }

        // 4.B: TỪ CHỐI KẾT BẠN
        else if (oldNotif.getType() == NotificationType.FRIEND_REQUEST) {
            // Từ chối thì không cần gọi DB Connection nữa (kệ cho nó PENDING mãi mãi hoặc xóa đi), chỉ việc xóa thông báo này.
            notificationRepository.delete(oldNotif);
            return "Decline friend request successfully!";
        }

        // 4.C. TỪ CHỐI LÊN SELLER (MỚI)
        else if (oldNotif.getType() == NotificationType.UPGRADE_REQUEST) {
            String bidderUsername = oldNotif.getTitle().replace("Yêu cầu lên Seller từ: ", "").trim();
            User bidder = userRepository.findByUserName(bidderUsername);

            if (bidder != null) {
                createNotification(bidder, null, NotificationType.AUCTION_FAILED,
                        "Từ chối yêu cầu lên Seller",
                        "Ban quản trị đã từ chối yêu cầu nâng cấp của bạn. Lý do: " + reason);
            }

            // Gửi thông báo cho TẤT CẢ các admin khác biết là ca này đã bị từ chối rồi, khỏi mất công check
            List<User> admins = userRepository.findByRole(Role.ADMIN);
            for (User admin : admins) {
                createNotification(admin, null, NotificationType.SYSTEM,
                        "Đã từ chối cấp quyền",
                        "Admin đã từ chối cấp quyền Seller cho user [" + bidderUsername + "]. Lý do: " + reason);
            }

            // Xóa rác: Quét sạch các thông báo chờ duyệt trong máy các Admin khác
            List<Notification> relatedNotifs = notificationRepository.findByTitle(oldNotif.getTitle());
            notificationRepository.deleteAll(relatedNotifs);

            return "Đã từ chối yêu cầu!";
        }

        throw new NotificationException(ErrorCode.NOTIFICATION_DECLINE_PAYMENT_INVALID);
    }

    // 5. Tạo notification mới <-> nguyên tắc DRY
    @Transactional
    public Notification createNotification(User user, Auction auction, NotificationType type, String title, String description) {
        log.debug("Tạo mới thông báo - User: {}, Type: {}, Title: {}", user.getUserName(), type, title);
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setAuction(auction); // Có thể NULL (ví dụ: thông báo kết bạn)
        notification.setType(type);
        notification.setTitle(title);
        notification.setDescription(description);
        notification.setRead(false);

        Notification savedNotification = notificationRepository.save(notification);
        log.debug("Đã lưu thành công thông báo ID: {}", savedNotification.getId());
        return savedNotification;
    }

    // 6. Xóa hết toàn bộ thông báo
    @Transactional
    public String deleteAllNotifications(String userName) {
        log.info("Thực hiện xóa tất cả thông báo cho user: {}", userName);
        List<Notification> allNotifs = notificationRepository.findByUserUserNameOrderByCreatedAtDesc(userName);

        List<Notification> toDelete = new ArrayList<>();
        for (Notification notif : allNotifs) {
            // BỔ SUNG UPGRADE_REQUEST VÀO DANH SÁCH GIỮ LẠI
            if (notif.getType() != NotificationType.PAYMENT_VERIFICATION &&
                    notif.getType() != NotificationType.FRIEND_REQUEST &&
                    notif.getType() != NotificationType.UPGRADE_REQUEST) {
                toDelete.add(notif);
            }
        }

        if (toDelete.isEmpty()) {
            return "No removable notifications found!";
        }

        notificationRepository.deleteAll(toDelete);
        log.info("Đã xóa {} thông báo rác cho user: {}", toDelete.size(), userName);
        return "Deleted " + toDelete.size() + " notifications successfully!";
    }
}