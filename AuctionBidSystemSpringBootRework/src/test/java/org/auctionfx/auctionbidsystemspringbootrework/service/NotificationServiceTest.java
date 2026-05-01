package org.auctionfx.auctionbidsystemspringbootrework.service;

import org.auctionfx.auctionbidsystemspringbootrework.dto.response.NotificationResponse;
import org.auctionfx.auctionbidsystemspringbootrework.entity.auction.Auction;
import org.auctionfx.auctionbidsystemspringbootrework.entity.item.Electronic;
import org.auctionfx.auctionbidsystemspringbootrework.entity.item.Item;
import org.auctionfx.auctionbidsystemspringbootrework.entity.notification.Notification;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Bidder;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.User;
import org.auctionfx.auctionbidsystemspringbootrework.enums.NotificationType;
import org.auctionfx.auctionbidsystemspringbootrework.exception.ErrorCode;
import org.auctionfx.auctionbidsystemspringbootrework.exception.NotificationException;
import org.auctionfx.auctionbidsystemspringbootrework.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AuctionService auctionService;

    @InjectMocks
    private NotificationService notificationService;

    private User mockUser;
    private Auction mockAuction;
    private Notification verificationNotif;
    private Notification successNotif;

    @BeforeEach
    void setUp() {
        mockUser = new Bidder();
        mockUser.setUserName("bidder_vip");

        Item mockItem = new Electronic();
        mockItem.setId("ITEM123456");
        mockItem.setName("iPhone 17 Pro Max");

        mockAuction = new Auction();
        mockAuction.setId("AUC-123");
        mockAuction.setBidProduct(mockItem);
        mockAuction.setHighestBid(new BigDecimal("39000000"));

        // Thông báo 1: PAYMENT_VERIFICATION
        verificationNotif = new Notification();
        verificationNotif.setId("NOTIF-VERIFY");
        verificationNotif.setType(NotificationType.PAYMENT_VERIFICATION);
        verificationNotif.setUser(mockUser);
        verificationNotif.setAuction(mockAuction);

        // Thông báo 2: AUCTION_SUCCESS
        successNotif = new Notification();
        successNotif.setId("NOTIF-SUCCESS");
        successNotif.setType(NotificationType.AUCTION_SUCCESS);
        successNotif.setTitle("Đấu giá thành công");
        successNotif.setCreatedAt(LocalDateTime.now());
    }

    // ========================================================
    // KỊCH BẢN 1: GET LIST NOTIFICATION
    // ========================================================
    @Test
    void getMyNotifications_Success() {
        when(notificationRepository.findByUserUserNameOrderByCreatedAtDesc("bidder_vip"))
                .thenReturn(List.of(successNotif));

        List<NotificationResponse> result = notificationService.getMyNotifications("bidder_vip");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Đấu giá thành công", result.get(0).getTitle());
        assertEquals(NotificationType.AUCTION_SUCCESS, result.get(0).getType());
    }

    // ========================================================
    // KỊCH BẢN 2: ACCEPT PAYMENT
    // ========================================================
    @Test
    void acceptPayment_ValidNotif_Success() {
        when(notificationRepository.findById("NOTIF-VERIFY")).thenReturn(Optional.of(verificationNotif));

        String result = notificationService.acceptPayment("NOTIF-VERIFY");

        // Cập nhật lại chuỗi so sánh bằng Tiếng Anh
        assertEquals("Accept payment successfully!", result);

        verify(auctionService, times(1)).acceptPayment("AUC-123");
        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(notificationRepository, times(1)).delete(verificationNotif);
    }

    // ========================================================
    // KỊCH BẢN 3: DECLINE PAYMENT
    // ========================================================
    @Test
    void declinePayment_ValidNotif_Success() {
        when(notificationRepository.findById("NOTIF-VERIFY")).thenReturn(Optional.of(verificationNotif));

        String result = notificationService.declinePayment("NOTIF-VERIFY");

        // Cập nhật lại chuỗi so sánh bằng Tiếng Anh
        assertEquals("Decline payment successfully!", result);

        // Cập nhật lại lời gọi hàm khớp với code mới của bạn
        verify(auctionService, times(1)).declinePayment("AUC-123");

        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(notificationRepository, times(1)).delete(verificationNotif);
    }

    // ========================================================
    // KỊCH BẢN 4: DELETE NOTIFICATION - THROW EXCEPTION
    // ========================================================
    @Test
    void deleteNotification_VerifyType_ThrowException() {
        when(notificationRepository.findById("NOTIF-VERIFY")).thenReturn(Optional.of(verificationNotif));

        // Cập nhật lại việc bắt lỗi NotificationException thay vì RuntimeException
        NotificationException exception = assertThrows(NotificationException.class, () -> {
            notificationService.deleteNotification("NOTIF-VERIFY");
        });

        // Kiểm tra xem mã lỗi ném ra có đúng là NOTIFICATION_DELETE_INVALID không
        assertEquals(ErrorCode.NOTIFICATION_DELETE_INVALID, exception.getErrorCode());

        verify(notificationRepository, times(0)).delete(any());
    }

    // ========================================================
    // KỊCH BẢN 5: DELETE NOTIFICATION - SUCCESS
    // ========================================================
    @Test
    void deleteNotification_NormalType_Success() {
        when(notificationRepository.findById("NOTIF-SUCCESS")).thenReturn(Optional.of(successNotif));

        String result = notificationService.deleteNotification("NOTIF-SUCCESS");

        // Cập nhật lại chuỗi so sánh
        assertEquals("Delete notification successfully!", result);

        verify(notificationRepository, times(1)).delete(successNotif);
    }
}