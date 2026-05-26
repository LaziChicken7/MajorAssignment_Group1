package org.auctionfx.auctionbidsystemspringbootrework.controller;

import lombok.extern.slf4j.Slf4j;
import org.auctionfx.auctionbidsystemspringbootrework.dto.response.ApiResponse;
import org.auctionfx.auctionbidsystemspringbootrework.dto.response.NotificationResponse;
import org.auctionfx.auctionbidsystemspringbootrework.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    // Lấy danh sách thông báo của một user
    @GetMapping("/{userName}")
    public ApiResponse<List<NotificationResponse>> getNotifications(@PathVariable String userName) {
        log.info("API Request: Lấy danh sách thông báo cho user [{}]", userName);
        ApiResponse<List<NotificationResponse>> apiResponse = new ApiResponse<>();
        apiResponse.setResult(notificationService.getMyNotifications(userName));
        return apiResponse;
    }

    // Nút thùng rác (Xóa thông báo)
    @DeleteMapping("/{notificationId}")
    public ApiResponse<String> deleteNotification(@PathVariable String notificationId) {
        log.info("API Request: Xóa thông báo ID [{}]", notificationId);
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(notificationService.deleteNotification(notificationId));
        return apiResponse;
    }

    // Nút Tích xanh: Chấp nhận thanh toán
    @PutMapping("/{notificationId}/accept")
    public ApiResponse<String> acceptNotification(@PathVariable String notificationId) {
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(notificationService.acceptNotification(notificationId));
        return apiResponse;
    }

    // Nút Chữ X đỏ: Từ chối (thanh toán / up seller)
    @PutMapping("/{notificationId}/decline")
    public ApiResponse<String> declineNotification(
            @PathVariable String notificationId,
            @RequestBody(required = false) java.util.Map<String, String> payload) {

        // Trích xuất lý do (nếu có)
        String reason = (payload != null && payload.containsKey("reason")) ? payload.get("reason") : "Không đủ điều kiện";

        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(notificationService.declineNotification(notificationId, reason));
        return apiResponse;
    }

    // Thêm vào trong NotificationController (Spring Boot)
    // Nút Xóa tất cả thông báo
    @DeleteMapping("/all/{userName}")
    public ApiResponse<String> deleteAllNotifications(@PathVariable String userName) {
        log.info("API Request: Xóa tất cả thông báo cho user [{}]", userName);
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(notificationService.deleteAllNotifications(userName));
        return apiResponse;
    }
}