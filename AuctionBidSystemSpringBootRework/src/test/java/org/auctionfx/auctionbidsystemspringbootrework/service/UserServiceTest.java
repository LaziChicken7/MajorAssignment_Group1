package org.auctionfx.auctionbidsystemspringbootrework.service;

import jakarta.persistence.EntityManager;
import org.auctionfx.auctionbidsystemspringbootrework.dto.request.LoginRequest;
import org.auctionfx.auctionbidsystemspringbootrework.dto.request.ResetPasswordRequest;
import org.auctionfx.auctionbidsystemspringbootrework.dto.request.UserCreationRequest;
import org.auctionfx.auctionbidsystemspringbootrework.dto.request.VerifyInfoRequest;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Admin;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Bidder;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Seller;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.User;
import org.auctionfx.auctionbidsystemspringbootrework.enums.NotificationType;
import org.auctionfx.auctionbidsystemspringbootrework.enums.Role;
import org.auctionfx.auctionbidsystemspringbootrework.exception.ErrorCode;
import org.auctionfx.auctionbidsystemspringbootrework.exception.UserException;
import org.auctionfx.auctionbidsystemspringbootrework.repository.BidderRepository;
import org.auctionfx.auctionbidsystemspringbootrework.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock private UserRepository userRepository;
    @Mock private BidderRepository bidderRepository;
    @Mock private EntityManager entityManager;
    @Mock private PasswordEncoder passwordEncoder;

    // THÊM MOCK CHO NOTIFICATION SERVICE (Vì các hàm Upgrade có gọi đến nó)
    @Mock private NotificationService notificationService;

    private Bidder testBidder;
    private Seller testSeller;
    private Admin testAdmin;

    @BeforeEach
    void setUp() {
        // Setup dữ liệu mẫu cho Bidder
        testBidder = new Bidder();
        testBidder.setId("bidder_1");
        testBidder.setUserName("bidder_pro");
        testBidder.setPassword("hashed_password");
        testBidder.setEmail("bidder@gmail.com");
        testBidder.setCitizenId("0123456789");
        testBidder.setFullName("Nguyễn Văn Đấu Giá");
        testBidder.setRole(Role.BIDDER);
        testBidder.setUserCode("BID1");
        testBidder.setBanned(false);

        // Setup dữ liệu mẫu cho Seller
        testSeller = new Seller();
        testSeller.setId("seller_1");
        testSeller.setUserName("seller_vip");
        testSeller.setRole(Role.SELLER);
        testSeller.setUserCode("SLR2");

        // Setup dữ liệu mẫu cho Admin
        testAdmin = new Admin();
        testAdmin.setId("admin_1");
        testAdmin.setUserName("admin_super");
        testAdmin.setRole(Role.ADMIN);
    }

    // ===================================================================================
    // 1. TEST CHO HÀM `createUser`
    // ===================================================================================

    @Test
    void createUser_Success_BidderRole() {
        UserCreationRequest request = new UserCreationRequest();
        request.setUserName("new_bidder");
        request.setPassword("123456");
        request.setEmail("new@gmail.com");
        request.setNumberPhone("0987654321");
        request.setCitizenId("111111111");
        request.setRole(Role.BIDDER);

        when(userRepository.existsByUserName(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByCitizenId(anyString())).thenReturn(false);
        when(userRepository.existsByNumberPhone(anyString())).thenReturn(false);
        when(userRepository.findMaxUserCodeNumber()).thenReturn(5);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_123456");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User createdUser = userService.createUser(request);

        assertNotNull(createdUser);
        assertTrue(createdUser instanceof Bidder);
        assertEquals("BID6", createdUser.getUserCode());
        assertEquals("hashed_123456", createdUser.getPassword());
        assertEquals("/uploads/images/avatar/avatarmacdinh.png", createdUser.getAvatarUrl());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void createUser_Fail_UsernameExisted() {
        UserCreationRequest request = new UserCreationRequest();
        request.setUserName("bidder_pro");
        when(userRepository.existsByUserName("bidder_pro")).thenReturn(true);

        UserException exception = assertThrows(UserException.class, () -> userService.createUser(request));
        assertEquals(ErrorCode.USERNAME_EXISTED, exception.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    // ===================================================================================
    // 2. TEST CHO HÀM `login`
    // ===================================================================================

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest();
        request.setUserName("bidder_pro");
        request.setPassword("raw_password");

        when(userRepository.findByUserName("bidder_pro")).thenReturn(testBidder);
        when(passwordEncoder.matches("raw_password", "hashed_password")).thenReturn(true);

        User loggedInUser = userService.login(request);
        assertEquals(testBidder, loggedInUser);
    }

    @Test
    void login_Fail_WrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setUserName("bidder_pro");
        request.setPassword("wrong_password");

        when(userRepository.findByUserName("bidder_pro")).thenReturn(testBidder);
        when(passwordEncoder.matches("wrong_password", "hashed_password")).thenReturn(false);

        UserException exception = assertThrows(UserException.class, () -> userService.login(request));
        assertEquals(ErrorCode.PASSWORD_NOT_MATCH, exception.getErrorCode());
    }

    @Test
    void login_Fail_UserBanned() {
        testBidder.setBanned(true);
        LoginRequest request = new LoginRequest();
        request.setUserName("bidder_pro");
        request.setPassword("raw_password");

        when(userRepository.findByUserName("bidder_pro")).thenReturn(testBidder);

        UserException exception = assertThrows(UserException.class, () -> userService.login(request));
        assertEquals(ErrorCode.USER_BANNED, exception.getErrorCode());
    }

    // ===================================================================================
    // 3. TEST CHO HÀM `upgradeBidderToSeller`
    // ===================================================================================

    @Test
    void upgradeBidderToSeller_Success() {
        when(userRepository.findByUserName("bidder_pro")).thenReturn(testBidder);
        when(userRepository.findById("bidder_1")).thenReturn(Optional.of(testSeller));

        // Cần Mock danh sách Admin để gửi thông báo không bị lỗi
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(testAdmin));

        String result = userService.upgradeBidderToSeller("bidder_pro");

        assertTrue(result.contains("Upgrade successfully!"));
        assertTrue(result.contains("SLR1"));

        verify(userRepository, times(1)).upgradeToSellerAndUpdateCode("bidder_1", "SLR1");
        verify(userRepository, times(1)).insertIntoSellersTable("bidder_1");
        verify(entityManager, times(1)).flush();
        verify(entityManager, times(1)).clear();

        // Kiểm tra xem đã bắn 2 thông báo chưa (1 cho Bidder, 1 cho các Admin khác)
        verify(notificationService, times(2)).createNotification(any(), isNull(), eq(NotificationType.AUCTION_SUCCESS), anyString(), anyString());
    }

    @Test
    void upgradeBidderToSeller_Fail_AlreadySeller() {
        when(userRepository.findByUserName("seller_vip")).thenReturn(testSeller);

        UserException exception = assertThrows(UserException.class, () -> userService.upgradeBidderToSeller("seller_vip"));
        assertEquals(ErrorCode.USER_ALREADY_SELLER, exception.getErrorCode());
    }

    // ===================================================================================
    // 4. TEST CHO HÀM `verifyUserInfo` VÀ `resetPassword`
    // ===================================================================================

    @Test
    void verifyAndResetPassword_Success() {
        VerifyInfoRequest verifyRequest = new VerifyInfoRequest();
        verifyRequest.setUserName("bidder_pro");
        verifyRequest.setEmail("bidder@gmail.com");
        verifyRequest.setCitizenId("0123456789");

        when(userRepository.findByUserName("bidder_pro")).thenReturn(testBidder);

        String generatedToken = userService.verifyUserInfo(verifyRequest);
        assertNotNull(generatedToken);

        ResetPasswordRequest resetRequest = new ResetPasswordRequest();
        resetRequest.setUserName("bidder_pro");
        resetRequest.setResetToken(generatedToken);
        resetRequest.setNewPassword("new_strong_password");

        when(passwordEncoder.encode("new_strong_password")).thenReturn("new_hashed");

        String result = userService.resetPassword(resetRequest);

        assertEquals("Password change successfully!", result);
        assertEquals("new_hashed", testBidder.getPassword());
        verify(userRepository, times(1)).save(testBidder);
        assertThrows(UserException.class, () -> userService.resetPassword(resetRequest));
    }

    @Test
    void verifyUserInfo_Fail_InfoNotMatch() {
        VerifyInfoRequest request = new VerifyInfoRequest();
        request.setUserName("bidder_pro");
        request.setEmail("hacker@gmail.com");
        request.setCitizenId("0123456789");

        when(userRepository.findByUserName("bidder_pro")).thenReturn(testBidder);

        UserException exception = assertThrows(UserException.class, () -> userService.verifyUserInfo(request));
        assertEquals(ErrorCode.USER_INFO_NOT_MATCH, exception.getErrorCode());
    }

    // ===================================================================================
    // 5. TEST CHO HÀM `toggleBanUser`
    // ===================================================================================

    @Test
    void toggleBanUser_Success() {
        when(userRepository.findByUserName("bidder_pro")).thenReturn(testBidder);

        String result = userService.toggleBanUser("bidder_pro");

        assertEquals("User has been banned!", result);
        assertTrue(testBidder.isBanned());
        verify(userRepository, times(1)).save(testBidder);
    }

    @Test
    void toggleBanUser_Fail_AdminCannotBeBanned() {
        when(userRepository.findByUserName("admin_super")).thenReturn(testAdmin);

        UserException exception = assertThrows(UserException.class, () -> userService.toggleBanUser("admin_super"));
        assertEquals(ErrorCode.BAN_USER_INVALID, exception.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    // ===================================================================================
    // 6. TEST CHO HÀM `removeAvatar`
    // ===================================================================================

    @Test
    void removeAvatar_Success() {
        testBidder.setAvatarUrl("/uploads/images/avatar/custom_avatar.png");
        when(userRepository.findByUserName("bidder_pro")).thenReturn(testBidder);

        String result = userService.removeAvatar("bidder_pro");

        assertEquals("/uploads/images/avatar/avatarmacdinh.png", result);
        assertEquals("/uploads/images/avatar/avatarmacdinh.png", testBidder.getAvatarUrl());
        verify(userRepository, times(1)).save(testBidder);
    }

    // ===================================================================================
    // 7. TEST CHO HÀM `requestUpgradeToSeller` (YÊU CẦU LÊN SELLER - TÍNH NĂNG MỚI)
    // ===================================================================================

    @Test
    void requestUpgradeToSeller_Success() {
        when(userRepository.findByUserName("bidder_pro")).thenReturn(testBidder);
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(testAdmin));

        String result = userService.requestUpgradeToSeller("bidder_pro");

        assertEquals("Đã gửi yêu cầu cấp quyền đến Ban quản trị! Vui lòng chờ phê duyệt.", result);
        verify(notificationService, times(1)).createNotification(
                eq(testAdmin), isNull(), eq(NotificationType.UPGRADE_REQUEST), anyString(), anyString()
        );
    }

    @Test
    void requestUpgradeToSeller_Fail_AlreadySeller() {
        when(userRepository.findByUserName("seller_vip")).thenReturn(testSeller);

        String result = userService.requestUpgradeToSeller("seller_vip");

        assertEquals("Bạn đã có quyền Seller hoặc Admin rồi!", result);
        verify(notificationService, never()).createNotification(any(), any(), any(), anyString(), anyString());
    }

    // ===================================================================================
    // 8. TEST CHO CÁC HÀM XỬ LÝ TRẠNG THÁI ONLINE/OFFLINE (TÍNH NĂNG MỚI)
    // ===================================================================================

    @Test
    void userOnlineOfflineStatus_Success() {
        when(userRepository.existsByUserName("bidder_pro")).thenReturn(true);

        // Báo Online
        userService.setUserOnline("bidder_pro");
        assertEquals("ONLINE", userService.getUserStatus("bidder_pro"));

        // Báo Offline
        userService.setUserOffline("bidder_pro");
        assertEquals("OFFLINE", userService.getUserStatus("bidder_pro"));
    }

    @Test
    void getUserStatus_Fail_UserNotFound() {
        when(userRepository.existsByUserName("ghost_user")).thenReturn(false);

        UserException exception = assertThrows(UserException.class, () -> userService.getUserStatus("ghost_user"));
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }
}