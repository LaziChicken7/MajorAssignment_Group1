package org.auctionfx.auctionbidsystemspringbootrework.service;

import org.auctionfx.auctionbidsystemspringbootrework.dto.request.ItemCancellationRequest;
import org.auctionfx.auctionbidsystemspringbootrework.dto.request.ItemCreationRequest;
import org.auctionfx.auctionbidsystemspringbootrework.entity.auction.Auction;
import org.auctionfx.auctionbidsystemspringbootrework.entity.item.Art;
import org.auctionfx.auctionbidsystemspringbootrework.entity.item.Item;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Bidder;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Seller;
import org.auctionfx.auctionbidsystemspringbootrework.enums.AuctionStatus;
import org.auctionfx.auctionbidsystemspringbootrework.enums.ItemType;
import org.auctionfx.auctionbidsystemspringbootrework.enums.NotificationType;
import org.auctionfx.auctionbidsystemspringbootrework.exception.ErrorCode;
import org.auctionfx.auctionbidsystemspringbootrework.exception.ItemException;
import org.auctionfx.auctionbidsystemspringbootrework.repository.AuctionRepository;
import org.auctionfx.auctionbidsystemspringbootrework.repository.ItemRepository;
import org.auctionfx.auctionbidsystemspringbootrework.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ItemServiceTest {

    @InjectMocks
    private ItemService itemService;

    @Mock private ItemRepository itemRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuctionRepository auctionRepository;
    @Mock private NotificationService notificationService;
    @Mock private PaymentService paymentService;

    private Seller testSeller;
    private Bidder testBidder;
    private Item testItem;
    private Auction testAuction;

    @BeforeEach
    void setUp() {
        // Mock dữ liệu Seller
        testSeller = new Seller();
        testSeller.setId("seller_1");
        testSeller.setUserName("seller_vip");

        // Mock dữ liệu Bidder (Người thắng cuộc)
        testBidder = new Bidder();
        testBidder.setId("bidder_1");
        testBidder.setUserName("bidder_pro");

        // Mock dữ liệu Item
        testItem = new Art();
        testItem.setId("item_1");
        testItem.setName("Mona Lisa Fake");
        testItem.setSeller(testSeller);
        testItem.setStartPrice(new BigDecimal("1000000"));

        // Mock dữ liệu Auction
        testAuction = new Auction();
        testAuction.setId("auction_1");
        testAuction.setBidProduct(testItem);
        testAuction.setSeller(testSeller);
        testAuction.setStatus(AuctionStatus.RUNNING);
        testAuction.setHighestBid(new BigDecimal("1500000"));
        testAuction.setWinningUser(testBidder);
    }

    // ===================================================================================
    // 1. TEST CHO HÀM `createItem`
    // ===================================================================================

    @Test
    void createItem_Success_ArtType() {
        // Arrange
        ItemCreationRequest request = new ItemCreationRequest();
        request.setSellerUserName("seller_vip");
        request.setItemType(ItemType.ART);
        request.setName("Beautiful Painting");
        request.setStartPrice(new BigDecimal("50000"));
        request.setNameAuthor("Leonardo");
        request.setCreationYear(1503);

        when(userRepository.findByUserName("seller_vip")).thenReturn(testSeller);

        // Act
        String result = itemService.createItem(request);

        // Assert
        assertTrue(result.contains("Create item successfully"));
        verify(itemRepository, times(1)).save(any(Art.class));
    }

    @Test
    void createItem_Fail_UserNotFound() {
        // Arrange
        ItemCreationRequest request = new ItemCreationRequest();
        request.setSellerUserName("unknown_user");

        when(userRepository.findByUserName("unknown_user")).thenReturn(null);

        // Act & Assert
        ItemException exception = assertThrows(ItemException.class, () -> itemService.createItem(request));
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(itemRepository, never()).save(any());
    }

    @Test
    void createItem_Fail_UserNotSeller() {
        // Arrange
        ItemCreationRequest request = new ItemCreationRequest();
        request.setSellerUserName("bidder_pro"); // Truyền vào tên của một Bidder thay vì Seller

        when(userRepository.findByUserName("bidder_pro")).thenReturn(testBidder);

        // Act & Assert
        ItemException exception = assertThrows(ItemException.class, () -> itemService.createItem(request));
        assertEquals(ErrorCode.SELLER_INVALID, exception.getErrorCode());
    }

    // ===================================================================================
    // 2. TEST CHO HÀM `cancelItemByAdmin`
    // ===================================================================================

    @Test
    void cancelItemByAdmin_Success_WithWinnerRefund() {
        // Arrange
        ItemCancellationRequest request = new ItemCancellationRequest();
        request.setReason("Hàng giả mạo");

        when(itemRepository.findById("item_1")).thenReturn(Optional.of(testItem));
        when(auctionRepository.findByBidProduct(testItem)).thenReturn(Optional.of(testAuction));

        // Act
        String result = itemService.cancelItemByAdmin("item_1", request);

        // Assert
        assertEquals("Item cancelled successfully by admin", result);
        assertEquals(AuctionStatus.CANCELLED, testAuction.getStatus());

        // Xác nhận đã gọi hàm hoàn tiền cho Winner
        verify(paymentService, times(1)).unFreezeMoney(testBidder.getId(), testAuction.getHighestBid());

        // Xác nhận đã gửi 2 thông báo (1 cho người bán, 1 cho người mua)
        verify(notificationService, times(1)).createNotification(
                eq(testSeller), eq(testAuction), eq(NotificationType.ITEM_CANCELLED_BY_ADMIN), anyString(), anyString()
        );
        verify(notificationService, times(1)).createNotification(
                eq(testBidder), eq(testAuction), eq(NotificationType.ITEM_CANCELLED_BY_ADMIN), anyString(), anyString()
        );
        verify(auctionRepository, times(1)).save(testAuction);
    }

    @Test
    void cancelItemByAdmin_Fail_AuctionAlreadyPaid() {
        // Arrange
        testAuction.setStatus(AuctionStatus.PAID);
        ItemCancellationRequest request = new ItemCancellationRequest();

        when(itemRepository.findById("item_1")).thenReturn(Optional.of(testItem));
        when(auctionRepository.findByBidProduct(testItem)).thenReturn(Optional.of(testAuction));

        // Act & Assert
        ItemException exception = assertThrows(ItemException.class, () -> itemService.cancelItemByAdmin("item_1", request));
        assertEquals(ErrorCode.CONDITION_CANCEL_AUCTION_INVALID, exception.getErrorCode());

        // Đảm bảo không có giao dịch hoàn tiền hay thông báo nào được chạy
        verify(paymentService, never()).unFreezeMoney(anyString(), any());
    }

    // ===================================================================================
    // 3. TEST CHO HÀM `uploadImagesForItem`
    // ===================================================================================

    @Test
    void uploadImagesForItem_Success() throws IOException {
        // Arrange
        when(itemRepository.findById("item_1")).thenReturn(Optional.of(testItem));

        // Giả lập 1 file upload
        MockMultipartFile mockFile = new MockMultipartFile(
                "files",
                "test-image.jpg",
                "image/jpeg",
                "test-image-content".getBytes()
        );
        MultipartFile[] files = { mockFile };

        // Act
        var resultUrls = itemService.uploadImagesForItem("item_1", files);

        // Assert
        assertNotNull(resultUrls);
        assertEquals(1, resultUrls.size());
        assertTrue(resultUrls.get(0).contains("/uploads/images/items/item_1/"));
        assertTrue(resultUrls.get(0).endsWith(".jpg"));

        verify(itemRepository, times(1)).save(testItem);
    }

    @Test
    void uploadImagesForItem_Fail_EmptyFiles() {
        // Arrange
        when(itemRepository.findById("item_1")).thenReturn(Optional.of(testItem));
        MultipartFile[] emptyFiles = new MultipartFile[0];

        // Act & Assert
        assertThrows(IOException.class, () -> itemService.uploadImagesForItem("item_1", emptyFiles));
    }

    // Cleanup thư mục tạm sau khi chạy test uploadImages (tránh rác project)
    @AfterEach
    void tearDown() {
        File dir = new File("uploads/images/items/item_1/");
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            dir.delete();
        }
    }
}