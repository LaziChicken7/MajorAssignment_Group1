package org.auctionfx.auctionbidsystemspringbootrework.service;

import org.auctionfx.auctionbidsystemspringbootrework.entity.auction.Auction;
import org.auctionfx.auctionbidsystemspringbootrework.entity.item.Item;
import org.auctionfx.auctionbidsystemspringbootrework.entity.notification.Notification;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Seller;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.User;
import org.auctionfx.auctionbidsystemspringbootrework.enums.NotificationType;
import org.auctionfx.auctionbidsystemspringbootrework.repository.NotificationRepository;
import org.auctionfx.auctionbidsystemspringbootrework.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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

    private Notification verificationNotif;
    private Notification friendRequestNotif;

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
        item.setId("ITEM_999"); // Bắt buộc ID phải từ 4 kí tự trở lên để hàm substring(0,4) không bị lỗi
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
    }

    // ========================================================
    // KỊCH BẢN: CHẤP NHẬN THANH TOÁN (ACCEPT PAYMENT)
    // ========================================================
    @Test
    void acceptNotification_PaymentVerification_Success() {
        // Arrange
        when(notificationRepository.findById("NOTIF-VERIFY")).thenReturn(Optional.of(verificationNotif));

        // FIX LỖI NPE: Dạy cho Mockito biết phải trả về 1 object có ID khi gọi hàm save()
        Notification mockSavedNotif = new Notification();
        mockSavedNotif.setId("SAVED-MOCK-ID");
        when(notificationRepository.save(any(Notification.class))).thenReturn(mockSavedNotif);

        // Act
        String result = notificationService.acceptNotification("NOTIF-VERIFY");

        // Assert
        assertEquals("Accept payment successfully!", result);
        verify(auctionService, times(1)).acceptPayment("AUC-123");
        verify(notificationRepository, times(2)).save(any(Notification.class));
        verify(notificationRepository, times(1)).delete(verificationNotif);
    }

    // ========================================================
    // KỊCH BẢN: TỪ CHỐI THANH TOÁN (DECLINE PAYMENT)
    // ========================================================
    @Test
    void declineNotification_PaymentVerification_Success() {
        // Arrange
        when(notificationRepository.findById("NOTIF-VERIFY")).thenReturn(Optional.of(verificationNotif));

        // FIX LỖI NPE: Dạy cho Mockito biết phải trả về 1 object có ID khi gọi hàm save()
        Notification mockSavedNotif = new Notification();
        mockSavedNotif.setId("SAVED-MOCK-ID");
        when(notificationRepository.save(any(Notification.class))).thenReturn(mockSavedNotif);

        // Act
        String result = notificationService.declineNotification("NOTIF-VERIFY");

        // Assert
        assertEquals("Decline payment successfully!", result);
        verify(auctionService, times(1)).declinePayment("AUC-123");
        verify(notificationRepository, times(2)).save(any(Notification.class));
        verify(notificationRepository, times(1)).delete(verificationNotif);
    }

    // ========================================================
    // KỊCH BẢN BỔ SUNG: CHẤP NHẬN KẾT BẠN (ACCEPT FRIEND REQUEST)
    // ========================================================
    @Test
    void acceptNotification_FriendRequest_Success() {
        // Arrange
        when(notificationRepository.findById("NOTIF-FRIEND")).thenReturn(Optional.of(friendRequestNotif));

        User sender = new User();
        sender.setUserName("sender_user");
        when(userRepository.findByUserName("sender_user")).thenReturn(sender);

        // FIX LỖI NPE: Dạy cho Mockito biết phải trả về 1 object có ID khi gọi hàm save()
        Notification mockSavedNotif = new Notification();
        mockSavedNotif.setId("SAVED-MOCK-ID");
        when(notificationRepository.save(any(Notification.class))).thenReturn(mockSavedNotif);

        // Act
        String result = notificationService.acceptNotification("NOTIF-FRIEND");

        // Assert
        assertEquals("Accept friend request successfully!", result);
        verify(chatService, times(1)).acceptFriendRequest("sender_user", "receiver_user");
        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(notificationRepository, times(1)).delete(friendRequestNotif);
    }

    // ========================================================
    // KỊCH BẢN BỔ SUNG: TỪ CHỐI KẾT BẠN (DECLINE FRIEND REQUEST)
    // ========================================================
    @Test
    void declineNotification_FriendRequest_Success() {
        // Arrange
        when(notificationRepository.findById("NOTIF-FRIEND")).thenReturn(Optional.of(friendRequestNotif));

        // Act
        String result = notificationService.declineNotification("NOTIF-FRIEND");

        // Assert
        assertEquals("Decline friend request successfully!", result);

        // Từ chối kết bạn chỉ cần xóa thông báo, không lưu thêm thông báo nào mới
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(notificationRepository, times(1)).delete(friendRequestNotif);
    }
}