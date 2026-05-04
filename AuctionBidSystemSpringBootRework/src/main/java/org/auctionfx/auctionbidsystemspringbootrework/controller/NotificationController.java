package org.auctionfx.auctionbidsystemspringbootrework.controller;

import org.auctionfx.auctionbidsystemspringbootrework.dto.response.ApiResponse;
import org.auctionfx.auctionbidsystemspringbootrework.dto.response.NotificationResponse;
import org.auctionfx.auctionbidsystemspringbootrework.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    @Autowired private NotificationService notificationService;

    // Lấy danh sách thông báo của một user
    @GetMapping("/{userName}")
    public List<NotificationResponse> getNotifications(@PathVariable String userName) {
        ApiResponse<List<NotificationResponse>> apiResponse = new ApiResponse<>();
        apiResponse.setResult(notificationService.getMyNotifications(userName));
        return apiResponse.getResult();
    }

    // Nút thùng rác (Xóa thông báo)
    @DeleteMapping("/{notificationId}")
    public ApiResponse<String> deleteNotification(@PathVariable String notificationId) {
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(notificationService.deleteNotification(notificationId));
        return apiResponse;
    }

    // Nút Tích xanh: Chấp nhận thanh toán
    @PutMapping("/{notificationId}/accept")
    public ApiResponse<String> acceptPayment(@PathVariable String notificationId) {
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(notificationService.acceptPayment(notificationId));
        return apiResponse;
    }

    // Nút Chữ X đỏ: Từ chối thanh toán
    @PutMapping("/{notificationId}/decline")
    public ApiResponse<String> declinePayment(@PathVariable String notificationId) {
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(notificationService.declinePayment(notificationId));
        return apiResponse;
    }
}
