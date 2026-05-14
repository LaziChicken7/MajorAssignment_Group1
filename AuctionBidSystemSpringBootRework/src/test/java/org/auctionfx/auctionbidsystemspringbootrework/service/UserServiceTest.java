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
        // Arrange
        UserCreationRequest request = new UserCreationRequest();
        request.setUserName("new_bidder");
        request.setPassword("123456");
        request.setEmail("new@gmail.com");
        request.setNumberPhone("0987654321");
        request.setCitizenId("111111111");
        request.setRole(Role.BIDDER);

        // Giả lập không có dữ liệu nào trùng lặp
        when(userRepository.existsByUserName(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByCitizenId(anyString())).thenReturn(false);
        when(userRepository.existsByNumberPhone(anyString())).thenReturn(false);

        // Giả lập ID tự tăng và Password Encoder
        when(userRepository.findMaxUserCodeNumber()).thenReturn(5);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_123456");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User createdUser = userService.createUser(request);

        // Assert
        assertNotNull(createdUser);
        assertTrue(createdUser instanceof Bidder);
        assertEquals("BID6", createdUser.getUserCode()); // Max = 5 -> Code tiếp theo là 6
        assertEquals("hashed_123456", createdUser.getPassword());
        assertEquals("/uploads/images/avatar/avatarmacdinh.png", createdUser.getAvatarUrl());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void createUser_Fail_UsernameExisted() {
        // Arrange
        UserCreationRequest request = new UserCreationRequest();
        request.setUserName("bidder_pro");

        when(userRepository.existsByUserName("bidder_pro")).thenReturn(true);

        // Act & Assert
        UserException exception = assertThrows(UserException.class, () -> userService.createUser(request));
        assertEquals(ErrorCode.USERNAME_EXISTED, exception.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    // ===================================================================================
    // 2. TEST CHO HÀM `login`
    // ===================================================================================

    @Test
    void login_Success() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUserName("bidder_pro");
        request.setPassword("raw_password");

        when(userRepository.findByUserName("bidder_pro")).thenReturn(testBidder);
        when(passwordEncoder.matches("raw_password", "hashed_password")).thenReturn(true);

        // Act
        User loggedInUser = userService.login(request);

        // Assert
        assertEquals(testBidder, loggedInUser);
    }

    @Test
    void login_Fail_WrongPassword() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUserName("bidder_pro");
        request.setPassword("wrong_password");

        when(userRepository.findByUserName("bidder_pro")).thenReturn(testBidder);
        when(passwordEncoder.matches("wrong_password", "hashed_password")).thenReturn(false);

        // Act & Assert
        UserException exception = assertThrows(UserException.class, () -> userService.login(request));
        assertEquals(ErrorCode.PASSWORD_NOT_MATCH, exception.getErrorCode());
    }

    @Test
    void login_Fail_UserBanned() {
        // Arrange
        testBidder.setBanned(true);
        LoginRequest request = new LoginRequest();
        request.setUserName("bidder_pro");
        request.setPassword("raw_password");

        when(userRepository.findByUserName("bidder_pro")).thenReturn(testBidder);

        // Act & Assert
        UserException exception = assertThrows(UserException.class, () -> userService.login(request));
        assertEquals(ErrorCode.USER_BANNED, exception.getErrorCode());
    }

    // ===================================================================================
    // 3. TEST CHO HÀM `upgradeBidderToSeller`
    // ===================================================================================

    @Test
    void upgradeBidderToSeller_Success() {
        // Arrange
        when(userRepository.findByUserName("bidder_pro")).thenReturn(testBidder);
        when(userRepository.findById("bidder_1")).thenReturn(Optional.of(testSeller)); // Trả về Seller sau khi EntityManager clear

        // Act
        String result = userService.upgradeBidderToSeller("bidder_pro");

        // Assert
        assertTrue(result.contains("Upgrade successfully!"));
        assertTrue(result.contains("SLR1")); // Từ BID1 -> SLR1

        // Kiểm tra xem các hàm SQL native và EntityManager đã được gọi để ép Spring Boot cập nhật bộ nhớ đệm chưa
        verify(userRepository, times(1)).upgradeToSellerAndUpdateCode("bidder_1", "SLR1");
        verify(userRepository, times(1)).insertIntoSellersTable("bidder_1");
        verify(entityManager, times(1)).flush();
        verify(entityManager, times(1)).clear();
    }

    @Test
    void upgradeBidderToSeller_Fail_AlreadySeller() {
        // Arrange
        when(userRepository.findByUserName("seller_vip")).thenReturn(testSeller);

        // Act & Assert
        UserException exception = assertThrows(UserException.class, () -> userService.upgradeBidderToSeller("seller_vip"));
        assertEquals(ErrorCode.USER_ALREADY_SELLER, exception.getErrorCode());
    }

    // ===================================================================================
    // 4. TEST CHO HÀM `verifyUserInfo` VÀ `resetPassword`
    // ===================================================================================

    @Test
    void verifyAndResetPassword_Success() {
        // ---- Bước 1: Verify ----
        VerifyInfoRequest verifyRequest = new VerifyInfoRequest();
        verifyRequest.setUserName("bidder_pro");
        verifyRequest.setEmail("bidder@gmail.com");
        verifyRequest.setCitizenId("0123456789");

        when(userRepository.findByUserName("bidder_pro")).thenReturn(testBidder);

        // Sinh Token
        String generatedToken = userService.verifyUserInfo(verifyRequest);
        assertNotNull(generatedToken);

        // ---- Bước 2: Đổi Mật Khẩu với Token vừa sinh ----
        ResetPasswordRequest resetRequest = new ResetPasswordRequest();
        resetRequest.setUserName("bidder_pro");
        resetRequest.setResetToken(generatedToken);
        resetRequest.setNewPassword("new_strong_password");

        when(passwordEncoder.encode("new_strong_password")).thenReturn("new_hashed");

        // Act
        String result = userService.resetPassword(resetRequest);

        // Assert
        assertEquals("Password change successfully!", result);
        assertEquals("new_hashed", testBidder.getPassword());
        verify(userRepository, times(1)).save(testBidder);

        // ---- Bước 3: Đảm bảo Token chỉ được dùng 1 lần ----
        assertThrows(UserException.class, () -> userService.resetPassword(resetRequest));
    }

    @Test
    void verifyUserInfo_Fail_InfoNotMatch() {
        // Arrange
        VerifyInfoRequest request = new VerifyInfoRequest();
        request.setUserName("bidder_pro");
        request.setEmail("hacker@gmail.com"); // Sai Email
        request.setCitizenId("0123456789");

        when(userRepository.findByUserName("bidder_pro")).thenReturn(testBidder);

        // Act & Assert
        UserException exception = assertThrows(UserException.class, () -> userService.verifyUserInfo(request));
        assertEquals(ErrorCode.USER_INFO_NOT_MATCH, exception.getErrorCode());
    }

    // ===================================================================================
    // 5. TEST CHO HÀM `toggleBanUser`
    // ===================================================================================

    @Test
    void toggleBanUser_Success() {
        // Arrange
        when(userRepository.findByUserName("bidder_pro")).thenReturn(testBidder);
        boolean initialBanState = testBidder.isBanned(); // Đang false

        // Act
        String result = userService.toggleBanUser("bidder_pro");

        // Assert
        assertEquals("User has been banned!", result);
        assertTrue(testBidder.isBanned()); // Trạng thái đã bị đảo ngược
        verify(userRepository, times(1)).save(testBidder);
    }

    @Test
    void toggleBanUser_Fail_AdminCannotBeBanned() {
        // Arrange
        when(userRepository.findByUserName("admin_super")).thenReturn(testAdmin);

        // Act & Assert
        UserException exception = assertThrows(UserException.class, () -> userService.toggleBanUser("admin_super"));
        assertEquals(ErrorCode.BAN_USER_INVALID, exception.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    // ===================================================================================
    // 6. TEST CHO HÀM `removeAvatar`
    // ===================================================================================

    @Test
    void removeAvatar_Success() {
        // Arrange
        testBidder.setAvatarUrl("/uploads/images/avatar/custom_avatar.png");
        when(userRepository.findByUserName("bidder_pro")).thenReturn(testBidder);

        // Act
        String result = userService.removeAvatar("bidder_pro");

        // Assert
        assertEquals("/uploads/images/avatar/avatarmacdinh.png", result);
        assertEquals("/uploads/images/avatar/avatarmacdinh.png", testBidder.getAvatarUrl());
        verify(userRepository, times(1)).save(testBidder);
    }
}