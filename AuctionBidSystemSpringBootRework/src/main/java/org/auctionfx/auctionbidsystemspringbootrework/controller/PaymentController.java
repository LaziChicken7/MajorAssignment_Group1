package org.auctionfx.auctionbidsystemspringbootrework.controller;

import lombok.extern.slf4j.Slf4j;
import org.auctionfx.auctionbidsystemspringbootrework.dto.response.ApiResponse;
import org.auctionfx.auctionbidsystemspringbootrework.dto.request.PaymentRequest;
import org.auctionfx.auctionbidsystemspringbootrework.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payments")
@Slf4j // Kích hoạt bộ ghi log của Lombok
public class PaymentController {
    @Autowired
    private PaymentService paymentService;

    // Nạp tiền
    @PostMapping("/deposit")
    public ApiResponse<String> deposit(@RequestBody PaymentRequest request) {
        log.info("API CALL: Yêu cầu NẠP {} VND vào tài khoản [{}]", request.getAmount(), request.getUserName());
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(paymentService.deposit(request.getUserName(), request.getAmount()));
        return apiResponse;
    }

    // Rút tiền
    @PostMapping("/withdraw")
    public ApiResponse<String> withdraw(@RequestBody PaymentRequest request) {
        log.info("API CALL: Yêu cầu RÚT {} VND từ tài khoản [{}]", request.getAmount(), request.getUserName());
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(paymentService.withdraw(request.getUserName(), request.getAmount()));
        return apiResponse;
    }

    // Trả về kết quả VÍ TIỀN cho JavaFX
    @GetMapping("/{userName}/history")
    public ApiResponse<Map<String, Object>> getWalletHistory(@PathVariable String userName) {
        log.debug("API CALL: Yêu cầu lấy thông tin Ví và Lịch sử giao dịch của User [{}]", userName);
        ApiResponse<Map<String, Object>> response = new ApiResponse<>();
        response.setResult(paymentService.getMyWalletAndHistory(userName));
        return response;
    }
}