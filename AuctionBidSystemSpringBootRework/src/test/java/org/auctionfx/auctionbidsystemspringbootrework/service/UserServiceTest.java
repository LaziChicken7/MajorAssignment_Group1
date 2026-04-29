package org.auctionfx.auctionbidsystemspringbootrework.service;

import org.auctionfx.auctionbidsystemspringbootrework.dto.request.UserCreationRequest;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.Bidder;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.User;
import org.auctionfx.auctionbidsystemspringbootrework.enums.Role;
import org.auctionfx.auctionbidsystemspringbootrework.exception.ErrorCode;
import org.auctionfx.auctionbidsystemspringbootrework.exception.UserException;
import org.auctionfx.auctionbidsystemspringbootrework.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class) // Báo cho Java biết ta sẽ sử dụng Diễn viên đóng thế (Mock)
class UserServiceTest {
    @Mock
    private UserRepository userRepository; // Tạo ra một "Thủ kho giả" (không nối vào MySQL)

    @InjectMocks
    private UserService userService; // Nhét Thủ kho giả vào Bếp trưởng UserService để test

    // Kịch bản 1: Tạo tài khoản thành công
    @Test
    void createUser_ValidRequest_Success() {
        // 1. Chuẩn bị dữ liệu đầu vào
        UserCreationRequest request = new UserCreationRequest();
        request.setUserName("test_user");
        request.setPassword("12345678");
        request.setEmail("test@gmail.com");
        request.setNumberPhone("012345");
        request.setCitizenId("001");
        request.setRole(Role.BIDDER);

        // 2. Dạy thủ kho cách trả lời
        // Khi Service hỏi: "Tên này tồn tại chưa", Thủ kho phải trả lời chưa (false)
        when(userRepository.existsByUserName(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByCitizenId(anyString())).thenReturn(false);
        when(userRepository.existsByNumberPhone(anyString())).thenReturn(false);

        // Trả về số đếm lớn nhất hiện tại là 5 (Để xem tạo ra có ra BID6 không)
        when(userRepository.findMaxUserCodeNumber()).thenReturn(5);

        // Khi gọi hàm save, trả về chính cái đối tượng đang muốn lưu
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 3. Chạy hàm cần test
        User result = userService.createUser(request);

        // 4. Kiểm tra kết quả (Assert)
        assertNotNull(result); // Kết quả không được phép Null
        assertEquals("test_user", result.getUserName()); // Tên phải khớp
        assertEquals("BID6", result.getUserCode()); // UserCode phải tự động thành BID6
        assertInstanceOf(Bidder.class, result); // Đối tượng phải sinh ra đúng Bidder
    }

    // Kịch bản 2: Test báo lỗi khi trùng tên đăng nhập
    @Test
    void createUser_UsernameExisted_ThrowsException() {
        // 1. Chuẩn bị dữ liệu đầu vào
        UserCreationRequest request = new UserCreationRequest();
        request.setUserName("trung_lap");

        // 2. Dạy thủ kho giả báo "Đã tồn tại"
        when(userRepository.existsByUserName("trung_lap")).thenReturn(true);

        // 3. Chạy và bắt lỗi
        // Kỳ vọng khi chạy lần này PHẢI ném ra Exception
        UserException exception = assertThrows(UserException.class, () -> {
            userService.createUser(request);
        });

        // Kiểm tra xem lỗi ném ra có đúng là USERNAME_EXISTED không?
        assertEquals(ErrorCode.USERNAME_EXISTED, exception.getErrorCode());
    }
}