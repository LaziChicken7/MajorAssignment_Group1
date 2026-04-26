package org.auctionfx.auctionbidsystemspringbootrework.controller;

import org.auctionfx.auctionbidsystemspringbootrework.dto.request.PaymentRequest;
import org.auctionfx.auctionbidsystemspringbootrework.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    @Autowired
    private PaymentService paymentService;

    @PostMapping("/deposit")
    public String deposit(@RequestBody PaymentRequest request) {
        return paymentService.deposit(request.getUserName(), request.getAmount());
    }

    @PostMapping("/withdraw")
    public String withdraw(@RequestBody PaymentRequest request) {
        return paymentService.withdraw(request.getUserName(), request.getAmount());
    }
}
