package org.auctionfx.auctionbidsystemspringbootrework.controller;

import org.auctionfx.auctionbidsystemspringbootrework.dto.request.ApiResponse;
import org.auctionfx.auctionbidsystemspringbootrework.dto.request.PaymentRequest;
import org.auctionfx.auctionbidsystemspringbootrework.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    @Autowired
    private PaymentService paymentService;

    // Nạp tiền
    @PostMapping("/deposit")
    public String deposit(@RequestBody PaymentRequest request) {
        return paymentService.deposit(request.getUserName(), request.getAmount());
    }

    // Rút tiền
    @PostMapping("/withdraw")
    public String withdraw(@RequestBody PaymentRequest request) {
        return paymentService.withdraw(request.getUserName(), request.getAmount());
    }

    // Trả về kết quả VÍ TIỀN cho JavaFX
    @GetMapping("/{userName}/history")
    public ApiResponse<Map<String, Object>> getWalletHistory(@PathVariable String userName) {
        ApiResponse<Map<String, Object>> response = new ApiResponse<>();
        response.setResult(paymentService.getMyWalletAndHistory(userName));
        return response;
    }
}
