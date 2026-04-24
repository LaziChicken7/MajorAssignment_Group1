package org.auctionfx.auctionbidsystemspringbootrework.controller;

import org.auctionfx.auctionbidsystemspringbootrework.dto.request.ApiResponse;
import org.auctionfx.auctionbidsystemspringbootrework.dto.request.PlaceBidRequest;
import org.auctionfx.auctionbidsystemspringbootrework.service.AuctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auctions")
public class AuctionController {
    @Autowired
    private AuctionService auctionService;

    @PostMapping("/{auctionId}/place-bid")
    public ApiResponse<String> placeBid(@PathVariable String auctionId, @RequestBody PlaceBidRequest request) {
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(auctionService.placeBid(auctionId, request.getBidderUserName(), request.getBidAmount()));
        return apiResponse;
    }

    @PostMapping("/{auctionId}/start")
    public ApiResponse<String> startAuction(@PathVariable String auctionId) {
        auctionService.startAuction(auctionId);
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult("Auction started successfully!");
        return apiResponse;
    }

    @PostMapping("/{auctionId}/close")
    public ApiResponse<String> closeAuction(@PathVariable String auctionId) {
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(auctionService.closeAuction(auctionId));
        return apiResponse;
    }

    @PostMapping("/{auctionId}/accept-payment")
    public ApiResponse<String> acceptPayment(@PathVariable String auctionId) {
        auctionService.acceptPayment(auctionId);
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult("Payment accepted and transferred successfully!");
        return apiResponse;
    }

    @PostMapping("/{auctionId}/cancel")
    public ApiResponse<String> cancelAuction(@PathVariable String auctionId) {
        auctionService.cancelAuction(auctionId);
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult("Auction cancelled successfully!");
        return apiResponse;
    }

}
