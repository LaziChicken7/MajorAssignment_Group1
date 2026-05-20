package org.auctionfx.auctionbidsystemspringbootrework.service;

import org.auctionfx.auctionbidsystemspringbootrework.dto.response.TransactionHistoryResponse;
import org.auctionfx.auctionbidsystemspringbootrework.entity.auction.Auction;
import org.auctionfx.auctionbidsystemspringbootrework.entity.item.Item;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Bidder;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.User;
import org.auctionfx.auctionbidsystemspringbootrework.enums.TransactionStatus;
import org.auctionfx.auctionbidsystemspringbootrework.exception.ErrorCode;
import org.auctionfx.auctionbidsystemspringbootrework.exception.PaymentException;
import org.auctionfx.auctionbidsystemspringbootrework.exception.UserException;
import org.auctionfx.auctionbidsystemspringbootrework.repository.AuctionRepository;
import org.auctionfx.auctionbidsystemspringbootrework.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuctionRepository auctionRepository;

    private Bidder testBuyer;
    private Bidder testSeller;
    private Auction testAuction;

    @BeforeEach
    void setUp() {
        // Setup Buyer (Người mua)
        testBuyer = new Bidder();
        testBuyer.setId("buyer_1");
        testBuyer.setUserName("buyer_vip");
        testBuyer.setMoneyOnWallet(new BigDecimal("100000"));
        testBuyer.setMoneyinFrozen(new BigDecimal("50000"));
        testBuyer.setBankAccountNumber("123456789");

        // Setup Seller (Người bán)
        testSeller = new Bidder(); // Trong logic của bạn, Seller cũng đc ép kiểu thành Bidder để lấy ví
        testSeller.setId("seller_1");
        testSeller.setUserName("seller_vip");
        testSeller.setMoneyOnWallet(new BigDecimal("200000"));
        testSeller.setMoneyinFrozen(new BigDecimal("0"));

        // Setup Auction & Item cho phần History
        Item testItem = new Item();
        testItem.setId("item_1");
        testItem.setName("MacBook Pro");

        testAuction = new Auction();
        testAuction.setId("auction_1");
        testAuction.setBidProduct(testItem);
        testAuction.setHighestBid(new BigDecimal("50000"));
        testAuction.setTransactionStatus(TransactionStatus.SUCCESS);
    }

    // ===================================================================================
    // 1. TEST ĐÓNG BĂNG TIỀN (freezeMoney)
    // ===================================================================================

    @Test
    void freezeMoney_Success() {
        // Arrange
        BigDecimal amountToFreeze = new BigDecimal("20000");
        when(userRepository.findById("buyer_1")).thenReturn(Optional.of(testBuyer));

        // Act
        String result = paymentService.freezeMoney("buyer_1", amountToFreeze);

        // Assert
        assertEquals("Freeze Money successfully!", result);
        assertEquals(new BigDecimal("80000"), testBuyer.getMoneyOnWallet()); // 100k - 20k
        assertEquals(new BigDecimal("70000"), testBuyer.getMoneyinFrozen()); // 50k + 20k
        verify(userRepository, times(1)).save(testBuyer);
    }

    @Test
    void freezeMoney_Fail_NotEnoughMoneyOnWallet() {
        // Arrange
        BigDecimal amountToFreeze = new BigDecimal("200000"); // Lớn hơn số dư 100k
        when(userRepository.findById("buyer_1")).thenReturn(Optional.of(testBuyer));

        // Act & Assert
        PaymentException exception = assertThrows(PaymentException.class, () ->
                paymentService.freezeMoney("buyer_1", amountToFreeze)
        );
        assertEquals(ErrorCode.NOT_ENOUGH_MONEY_ON_WALLET, exception.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void freezeMoney_Fail_UserNotFound() {
        // Arrange
        when(userRepository.findById("unknown")).thenReturn(Optional.empty());

        // Act & Assert
        UserException exception = assertThrows(UserException.class, () ->
                paymentService.freezeMoney("unknown", new BigDecimal("10000"))
        );
        assertEquals(ErrorCode.USER_INVALID, exception.getErrorCode());
    }

    // ===================================================================================
    // 2. TEST HOÀN TIỀN (unFreezeMoney)
    // ===================================================================================

    @Test
    void unFreezeMoney_Success() {
        // Arrange
        BigDecimal amountToUnfreeze = new BigDecimal("30000");
        when(userRepository.findById("buyer_1")).thenReturn(Optional.of(testBuyer));

        // Act
        String result = paymentService.unFreezeMoney("buyer_1", amountToUnfreeze);

        // Assert
        assertEquals("Unfreeze Money successfully!", result);
        assertEquals(new BigDecimal("130000"), testBuyer.getMoneyOnWallet()); // 100k + 30k
        assertEquals(new BigDecimal("20000"), testBuyer.getMoneyinFrozen()); // 50k - 30k
        verify(userRepository, times(1)).save(testBuyer);
    }

    @Test
    void unFreezeMoney_Fail_NotEnoughMoneyInFrozen() {
        // Arrange
        BigDecimal amountToUnfreeze = new BigDecimal("100000"); // Lớn hơn số dư đóng băng 50k
        when(userRepository.findById("buyer_1")).thenReturn(Optional.of(testBuyer));

        // Act & Assert
        PaymentException exception = assertThrows(PaymentException.class, () ->
                paymentService.unFreezeMoney("buyer_1", amountToUnfreeze)
        );
        assertEquals(ErrorCode.NOT_ENOUGH_MONEY_IN_FROZEN, exception.getErrorCode());
    }

    // ===================================================================================
    // 3. TEST CHUYỂN TIỀN (transferMoney)
    // ===================================================================================

    @Test
    void transferMoney_Success() {
        // Arrange
        BigDecimal amountToTransfer = new BigDecimal("40000");
        when(userRepository.findById("buyer_1")).thenReturn(Optional.of(testBuyer));
        when(userRepository.findById("seller_1")).thenReturn(Optional.of(testSeller));

        // Act
        String result = paymentService.transferMoney("buyer_1", "seller_1", amountToTransfer);

        // Assert
        assertEquals("Transfer Money successfully!", result);
        assertEquals(new BigDecimal("10000"), testBuyer.getMoneyinFrozen()); // Trừ 40k của buyer
        assertEquals(new BigDecimal("240000"), testSeller.getMoneyOnWallet()); // Cộng 40k cho seller

        verify(userRepository, times(1)).save(testBuyer);
        verify(userRepository, times(1)).save(testSeller);
    }

    // ===================================================================================
    // 4. TEST NẠP / RÚT TIỀN (deposit / withdraw)
    // ===================================================================================

    @Test
    void deposit_Success() {
        // Arrange
        when(userRepository.findByUserName("buyer_vip")).thenReturn(testBuyer);

        // Act
        String result = paymentService.deposit("buyer_vip", new BigDecimal("50000"));

        // Assert
        assertTrue(result.contains("Deposit successfully"));
        assertEquals(new BigDecimal("150000"), testBuyer.getMoneyOnWallet());
        verify(userRepository, times(1)).save(testBuyer);
    }

    @Test
    void withdraw_Success() {
        // Arrange
        when(userRepository.findByUserName("buyer_vip")).thenReturn(testBuyer);

        // Act
        String result = paymentService.withdraw("buyer_vip", new BigDecimal("40000"));

        // Assert
        assertTrue(result.contains("Withdraw successfully"));
        assertEquals(new BigDecimal("60000"), testBuyer.getMoneyOnWallet());
        verify(userRepository, times(1)).save(testBuyer);
    }

    @Test
    void withdraw_Fail_InvalidAmount() {
        // Arrange & Act & Assert
        PaymentException exception = assertThrows(PaymentException.class, () ->
                paymentService.withdraw("buyer_vip", new BigDecimal("-10000"))
        );
        assertEquals(ErrorCode.WITHDRAW_MONEY_INVALID, exception.getErrorCode());
    }

    // ===================================================================================
    // 5. TEST LẤY LỊCH SỬ GIAO DỊCH (getMyWalletAndHistory)
    // ===================================================================================

    @Test
    @SuppressWarnings("unchecked") // Bỏ qua warning ép kiểu List trong test
    void getMyWalletAndHistory_Success() {
        // Arrange
        when(userRepository.findByUserName("buyer_vip")).thenReturn(testBuyer);

        // 1. Mock hàm findWonAuctions (Chỉ có 2 tham số, truyền thẳng giá trị)
        when(auctionRepository.findWonAuctions("buyer_vip", TransactionStatus.SUCCESS))
                .thenReturn(Collections.singletonList(testAuction));

        // 2. Mock hàm findLostAuctions (Có 3 tham số, tham số cuối dùng any() vì ta không biết chính xác service gọi Enum gì)
        // Lưu ý: Đã dùng any() thì các tham số kia phải bọc trong eq()
        when(auctionRepository.findLostAuctions(eq("buyer_vip"), eq(TransactionStatus.FAILED), any()))
                .thenReturn(Collections.emptyList());

        // Act
        Map<String, Object> response = paymentService.getMyWalletAndHistory("buyer_vip");

        // Assert
        assertNotNull(response);
        assertEquals("123456789", response.get("bankAccountNumber"));
        assertEquals(new BigDecimal("100000"), response.get("moneyOnWallet"));
        assertEquals(new BigDecimal("50000"), response.get("moneyinFrozen"));

        List<TransactionHistoryResponse> successList = (List<TransactionHistoryResponse>) response.get("successTransaction");
        List<TransactionHistoryResponse> failedList = (List<TransactionHistoryResponse>) response.get("failedTransaction");

        assertEquals(1, successList.size());
        assertEquals("item_1", successList.get(0).getItemId()); // Đảm bảo lấy đúng ID sản phẩm
        assertEquals(0, failedList.size());
    }
}