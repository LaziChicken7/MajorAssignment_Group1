package org.auctionfx.auctionbidsystemspringbootrework.service;

import org.auctionfx.auctionbidsystemspringbootrework.dto.request.AuctionCreationRequest;
import org.auctionfx.auctionbidsystemspringbootrework.entity.auction.Auction;
import org.auctionfx.auctionbidsystemspringbootrework.entity.auction.AutoBidConfig;
import org.auctionfx.auctionbidsystemspringbootrework.entity.auction.BidTransaction;
import org.auctionfx.auctionbidsystemspringbootrework.entity.item.Item;
import org.auctionfx.auctionbidsystemspringbootrework.entity.notification.Notification;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Bidder;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Seller;
import org.auctionfx.auctionbidsystemspringbootrework.enums.AuctionStatus;
import org.auctionfx.auctionbidsystemspringbootrework.exception.AuctionException;
import org.auctionfx.auctionbidsystemspringbootrework.exception.ErrorCode;
import org.auctionfx.auctionbidsystemspringbootrework.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuctionServiceTest {

    @InjectMocks
    private AuctionService auctionService;

    @Mock private AuctionRepository auctionRepository;
    @Mock private UserRepository userRepository;
    @Mock private BidTransactionRepository bidTransactionRepository;
    @Mock private PaymentService paymentService;
    @Mock private ItemRepository itemRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private AutoBidConfigRepository autoBidConfigRepository;

    private Auction testAuction;
    private Bidder testBidder;
    private Seller testSeller;
    private Item testItem;

    @BeforeEach
    void setUp() {
        // Setup dữ liệu mẫu (Mock Data) trước mỗi test case
        testSeller = new Seller();
        testSeller.setId("seller_1");
        testSeller.setUserName("seller_vip");

        testBidder = new Bidder();
        testBidder.setId("bidder_1");
        testBidder.setUserName("bidder_pro");
        testBidder.setBanned(false);

        testItem = new Item();
        testItem.setId("item_1");
        testItem.setName("Rolex Watch");
        testItem.setSeller(testSeller);
        testItem.setStartPrice(new BigDecimal("50000"));

        testAuction = new Auction();
        testAuction.setId("auction_1");
        testAuction.setBidProduct(testItem);
        testAuction.setSeller(testSeller);
        testAuction.setStatus(AuctionStatus.RUNNING);
        testAuction.setStartTime(LocalDateTime.now().minusDays(1));
        testAuction.setEndTime(LocalDateTime.now().plusDays(1)); // Còn thời gian
        testAuction.setHighestBid(new BigDecimal("50000"));
        testAuction.setStepPrice(new BigDecimal("1000"));
    }

    // ===================================================================================
    // 1. TEST CHO HÀM `placeBid`
    // ===================================================================================

    @Test
    void placeBid_Success_NoSnipping() {
        // Arrange (Chuẩn bị)
        BigDecimal bidAmount = new BigDecimal("60000");
        when(auctionRepository.findById("auction_1")).thenReturn(Optional.of(testAuction));
        when(userRepository.findByUserName("bidder_pro")).thenReturn(testBidder);
        when(autoBidConfigRepository.findByAuctionAndIsActiveTrueOrderByCreatedAtAsc(testAuction))
                .thenReturn(new ArrayList<>()); // Trả về list rỗng để bỏ qua vòng lặp AutoBid

        // Act (Hành động)
        String result = auctionService.placeBid("auction_1", "bidder_pro", bidAmount);

        // Assert (Kiểm tra)
        assertTrue(result.contains("Bid successfully with money: 60000"));
        assertEquals(bidAmount, testAuction.getHighestBid());
        assertEquals(testBidder, testAuction.getWinningUser());

        // Kiểm tra xem hàm đóng băng tiền và lưu giao dịch có được gọi không
        verify(paymentService, times(1)).freezeMoney(testBidder.getId(), bidAmount);
        verify(bidTransactionRepository, times(1)).save(any(BidTransaction.class));
        verify(auctionRepository, times(1)).save(testAuction);
    }

    @Test
    void placeBid_Fail_AuctionNotRunning() {
        // Arrange
        testAuction.setStatus(AuctionStatus.FINISHED);
        when(auctionRepository.findById("auction_1")).thenReturn(Optional.of(testAuction));
        when(userRepository.findByUserName("bidder_pro")).thenReturn(testBidder);

        // Act & Assert
        AuctionException exception = assertThrows(AuctionException.class, () -> {
            auctionService.placeBid("auction_1", "bidder_pro", new BigDecimal("60000"));
        });
        assertEquals(ErrorCode.AUCTION_NOT_RUNNING, exception.getErrorCode());
        verify(paymentService, never()).freezeMoney(any(), any()); // Không bị trừ tiền
    }

    @Test
    void placeBid_Fail_BidAmountTooLow() {
        // Arrange
        BigDecimal lowAmount = new BigDecimal("40000"); // Nhỏ hơn giá hiện tại (50000)
        when(auctionRepository.findById("auction_1")).thenReturn(Optional.of(testAuction));
        when(userRepository.findByUserName("bidder_pro")).thenReturn(testBidder);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            auctionService.placeBid("auction_1", "bidder_pro", lowAmount);
        });
        assertTrue(exception.getMessage().contains("Money must be higher than now"));
    }

    @Test
    void placeBid_Fail_SellerSelfBidding() {
        // Arrange
        when(auctionRepository.findById("auction_1")).thenReturn(Optional.of(testAuction));
        when(userRepository.findByUserName("seller_vip")).thenReturn(testSeller);

        // Act & Assert
        AuctionException exception = assertThrows(AuctionException.class, () -> {
            auctionService.placeBid("auction_1", "seller_vip", new BigDecimal("60000"));
        });
        assertEquals(ErrorCode.AUCTION_BIDDER_INVALID, exception.getErrorCode());
    }

    // ===================================================================================
    // 2. TEST CHO HÀM `closeAuction`
    // ===================================================================================

    @Test
    void closeAuction_WithWinner_Success() {
        // Arrange
        testAuction.setWinningUser(testBidder);
        when(auctionRepository.findById("auction_1")).thenReturn(Optional.of(testAuction));

        // Act
        String result = auctionService.closeAuction("auction_1");

        // Assert
        assertEquals(AuctionStatus.FINISHED, testAuction.getStatus());
        assertTrue(result.contains("Winner is:"));
        verify(notificationRepository, times(2)).save(any(Notification.class)); // 1 cho winner, 1 cho seller
        verify(auctionRepository, times(1)).save(testAuction);
    }

    @Test
    void closeAuction_NoWinner_Success() {
        // Arrange
        testAuction.setWinningUser(null);
        when(auctionRepository.findById("auction_1")).thenReturn(Optional.of(testAuction));

        // Act
        String result = auctionService.closeAuction("auction_1");

        // Assert
        assertEquals(AuctionStatus.FINISHED, testAuction.getStatus());
        assertTrue(result.contains("No one winner"));
        verify(notificationRepository, times(1)).save(any(Notification.class)); // Báo ế cho seller
        verify(auctionRepository, times(1)).save(testAuction);
    }

    // ===================================================================================
    // 3. TEST CHO HÀM `createAuction`
    // ===================================================================================

    @Test
    void createAuction_Success() {
        // Arrange
        AuctionCreationRequest request = new AuctionCreationRequest();
        request.setItemId("item_1");
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(3));

        when(itemRepository.findById("item_1")).thenReturn(Optional.of(testItem));

        // Act
        String result = auctionService.createAuction(request);

        // Assert
        assertTrue(result.contains("Create Auction item successfully!"));
        verify(auctionRepository, times(1)).save(any(Auction.class));
    }

    @Test
    void createAuction_Fail_ItemNotFound() {
        // Arrange
        AuctionCreationRequest request = new AuctionCreationRequest();
        request.setItemId("invalid_item");
        when(itemRepository.findById("invalid_item")).thenReturn(Optional.empty());

        // Act & Assert
        AuctionException exception = assertThrows(AuctionException.class, () -> {
            auctionService.createAuction(request);
        });
        assertEquals(ErrorCode.ITEM_NOT_FOUND, exception.getErrorCode());
    }
}