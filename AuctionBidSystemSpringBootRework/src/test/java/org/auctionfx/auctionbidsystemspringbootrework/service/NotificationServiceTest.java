package org.auctionfx.auctionbidsystemspringbootrework.service;

import org.auctionfx.auctionbidsystemspringbootrework.entity.auction.Auction;
import org.auctionfx.auctionbidsystemspringbootrework.entity.item.Item;
import org.auctionfx.auctionbidsystemspringbootrework.entity.notification.Notification;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Seller;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.User;
import org.auctionfx.auctionbidsystemspringbootrework.enums.NotificationType;
import org.auctionfx.auctionbidsystemspringbootrework.enums.Role;
import org.auctionfx.auctionbidsystemspringbootrework.repository.NotificationRepository;
import org.auctionfx.auctionbidsystemspringbootrework.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuctionService auctionService;

    @Mock
    private ChatService chatService;

    // THÊM MOCK CHO UserService VÌ TÍNH NĂNG UPGRADE SELLER CẦN GỌI ĐẾN NÓ
    @Mock
    private UserService userService;

    private Notification verificationNotif;
    private Notification friendRequestNotif;
    private Notification upgradeRequestNotif; // Thông báo yêu cầu lên Seller

    @BeforeEach
    void setUp() {
        // -----------------------------------------------------
        // SETUP: DỮ LIỆU GIẢ CHO THANH TOÁN (PAYMENT VERIFICATION)
        // -----------------------------------------------------
        User buyer = new User();
        buyer.setUserName("buyer123");
        buyer.setFullName("Nguyễn Văn Mua");

        Seller seller = new Seller();
        seller.setUserName("seller123");
        seller.setFullName("Trần Văn Bán");

        Item item = new Item();
        item.setId("ITEM_999");
        item.setName("iPhone 15 Pro Max");

        Auction auction = new Auction();
        auction.setId("AUC-123");
        auction.setHighestBid(new BigDecimal("25000000"));
        auction.setBidProduct(item);
        auction.setSeller(seller);

        verificationNotif = new Notification();
        verificationNotif.setId("NOTIF-VERIFY");
        verificationNotif.setType(NotificationType.PAYMENT_VERIFICATION);
        verificationNotif.setUser(buyer);
        verificationNotif.setAuction(auction);

        // -----------------------------------------------------
        // SETUP: DỮ LIỆU GIẢ CHO KẾT BẠN (FRIEND REQUEST)
        // -----------------------------------------------------
        User receiver = new User();
        receiver.setUserName("receiver_user");

        friendRequestNotif = new Notification();
        friendRequestNotif.setId("NOTIF-FRIEND");
        friendRequestNotif.setType(NotificationType.FRIEND_REQUEST);
        friendRequestNotif.setTitle("Yêu cầu kết bạn từ: sender_user");
        friendRequestNotif.setUser(receiver);

        // -----------------------------------------------------
        // SETUP: DỮ LIỆU GIẢ CHO YÊU CẦU LÊN SELLER (UPGRADE REQUEST)
        // -----------------------------------------------------
        upgradeRequestNotif = new Notification();
        upgradeRequestNotif.setId("NOTIF-UPGRADE");
        upgradeRequestNotif.setType(NotificationType.UPGRADE_REQUEST);
        upgradeRequestNotif.setTitle("Yêu cầu lên Seller từ: bidder_user");
        upgradeRequestNotif.setUser(receiver); // Receiver ở đây đóng vai trò là 1 Admin nhận được thông báo
    }

    // ========================================================
    // KỊCH BẢN 1: CHẤP NHẬN THANH TOÁN
    // ========================================================
    @Test
    void acceptNotification_PaymentVerification_Success() {
        when(notificationRepository.findById("NOTIF-VERIFY")).thenReturn(Optional.of(verificationNotif));

        Notification mockSavedNotif = new Notification();
        mockSavedNotif.setId("SAVED-MOCK-ID");
        when(notificationRepository.save(any(Notification.class))).thenReturn(mockSavedNotif);

        String result = notificationService.acceptNotification("NOTIF-VERIFY");

        assertEquals("Accept payment successfully!", result);
        verify(auctionService, times(1)).acceptPayment("AUC-123");
        verify(notificationRepository, times(2)).save(any(Notification.class));
        verify(notificationRepository, times(1)).delete(verificationNotif);
    }

    // ========================================================
    // KỊCH BẢN 2: TỪ CHỐI THANH TOÁN
    // ========================================================
    @Test
    void declineNotification_PaymentVerification_Success() {
        when(notificationRepository.findById("NOTIF-VERIFY")).thenReturn(Optional.of(verificationNotif));

        Notification mockSavedNotif = new Notification();
        mockSavedNotif.setId("SAVED-MOCK-ID");
        when(notificationRepository.save(any(Notification.class))).thenReturn(mockSavedNotif);

        // Đã sửa: Truyền thêm tham số "reason" (dù logic cũ ko dùng tới)
        String result = notificationService.declineNotification("NOTIF-VERIFY", "Tôi không muốn mua nữa");

        assertEquals("Decline payment successfully!", result);
        verify(auctionService, times(1)).declinePayment("AUC-123");
        verify(notificationRepository, times(2)).save(any(Notification.class));
        verify(notificationRepository, times(1)).delete(verificationNotif);
    }

    // ========================================================
    // KỊCH BẢN 3: CHẤP NHẬN KẾT BẠN
    // ========================================================
    @Test
    void acceptNotification_FriendRequest_Success() {
        when(notificationRepository.findById("NOTIF-FRIEND")).thenReturn(Optional.of(friendRequestNotif));

        User sender = new User();
        sender.setUserName("sender_user");
        when(userRepository.findByUserName("sender_user")).thenReturn(sender);

        Notification mockSavedNotif = new Notification();
        mockSavedNotif.setId("SAVED-MOCK-ID");
        when(notificationRepository.save(any(Notification.class))).thenReturn(mockSavedNotif);

        String result = notificationService.acceptNotification("NOTIF-FRIEND");

        assertEquals("Accept friend request successfully!", result);
        verify(chatService, times(1)).acceptFriendRequest("sender_user", "receiver_user");
        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(notificationRepository, times(1)).delete(friendRequestNotif);
    }

    // ========================================================
    // KỊCH BẢN 4: TỪ CHỐI KẾT BẠN
    // ========================================================
    @Test
    void declineNotification_FriendRequest_Success() {
        when(notificationRepository.findById("NOTIF-FRIEND")).thenReturn(Optional.of(friendRequestNotif));

        // Đã sửa: Truyền thêm tham số "reason"
        String result = notificationService.declineNotification("NOTIF-FRIEND", "Không thích kết bạn");

        assertEquals("Decline friend request successfully!", result);
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(notificationRepository, times(1)).delete(friendRequestNotif);
    }

    // ========================================================
    // KỊCH BẢN 5: DUYỆT CẤP QUYỀN SELLER (MỚI)
    // ========================================================
    @Test
    void acceptNotification_UpgradeRequest_Success() {
        when(notificationRepository.findById("NOTIF-UPGRADE")).thenReturn(Optional.of(upgradeRequestNotif));

        // Giả lập tìm thấy 3 thông báo chờ duyệt trong máy các Admin khác
        when(notificationRepository.findByTitle("Yêu cầu lên Seller từ: bidder_user"))
                .thenReturn(List.of(upgradeRequestNotif, new Notification(), new Notification()));

        String result = notificationService.acceptNotification("NOTIF-UPGRADE");

        assertEquals("Đã phê duyệt yêu cầu lên Seller!", result);
        verify(userService, times(1)).upgradeBidderToSeller("bidder_user");
        verify(notificationRepository, times(1)).deleteAll(anyList()); // Đảm bảo đã gọi hàm dọn dẹp hòm thư
    }

    // ========================================================
    // KỊCH BẢN 6: TỪ CHỐI CẤP QUYỀN SELLER KÈM LÝ DO (MỚI)
    // ========================================================
    @Test
    void declineNotification_UpgradeRequest_Success() {
        when(notificationRepository.findById("NOTIF-UPGRADE")).thenReturn(Optional.of(upgradeRequestNotif));

        User bidder = new User();
        bidder.setUserName("bidder_user");
        when(userRepository.findByUserName("bidder_user")).thenReturn(bidder);

        User admin1 = new User();
        admin1.setUserName("admin1");
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(admin1));

        Notification mockSavedNotif = new Notification();
        mockSavedNotif.setId("SAVED-MOCK-ID");
        when(notificationRepository.save(any(Notification.class))).thenReturn(mockSavedNotif);

        // Giả lập tìm thấy thông báo để xóa
        when(notificationRepository.findByTitle("Yêu cầu lên Seller từ: bidder_user"))
                .thenReturn(List.of(upgradeRequestNotif));

        // Thực hiện từ chối
        String reason = "Thông tin không rõ ràng";
        String result = notificationService.declineNotification("NOTIF-UPGRADE", reason);

        assertEquals("Đã từ chối yêu cầu!", result);

        // Sẽ lưu 2 thông báo: 1 cho Bidder báo thất bại, 1 cho các Admin báo là đã xử lý
        verify(notificationRepository, times(2)).save(any(Notification.class));
        verify(notificationRepository, times(1)).deleteAll(anyList()); // Đảm bảo đã xóa rác
    }
}