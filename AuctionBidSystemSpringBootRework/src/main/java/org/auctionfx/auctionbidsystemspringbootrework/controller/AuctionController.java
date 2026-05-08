package org.auctionfx.auctionbidsystemspringbootrework.controller;

import org.auctionfx.auctionbidsystemspringbootrework.dto.response.ApiResponse;
import org.auctionfx.auctionbidsystemspringbootrework.dto.request.AuctionCreationRequest;
import org.auctionfx.auctionbidsystemspringbootrework.dto.request.PlaceBidRequest;
import org.auctionfx.auctionbidsystemspringbootrework.entity.auction.Auction;
import org.auctionfx.auctionbidsystemspringbootrework.entity.auction.BidTransaction;
import org.auctionfx.auctionbidsystemspringbootrework.service.AuctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PutMapping("/{auctionId}/start")
    public ApiResponse<String> startAuction(@PathVariable String auctionId) {
        auctionService.startAuction(auctionId);
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult("Auction started successfully!");
        return apiResponse;
    }

    @PutMapping("/{auctionId}/close")
    public ApiResponse<String> closeAuction(@PathVariable String auctionId) {
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(auctionService.closeAuction(auctionId));
        apiResponse.setResult("Auction closed successfully!");
        return apiResponse;
    }

    @PutMapping("/{auctionId}/accept-payment")
    public ApiResponse<String> acceptPayment(@PathVariable String auctionId) {
        auctionService.acceptPayment(auctionId);
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult("Payment accepted and transferred successfully!");
        return apiResponse;
    }

    @PutMapping("/{auctionId}/decline-payment")
    public ApiResponse<String> declinePayment(@PathVariable String auctionId) {
        auctionService.declinePayment(auctionId);
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult("Payment accepted and transferred successfully!");
        return apiResponse;
    }

    @PutMapping("/{auctionId}/cancel")
    public ApiResponse<String> cancelAuction(@PathVariable String auctionId) {
        auctionService.cancelAuction(auctionId);
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult("Auction cancelled successfully!");
        return apiResponse;
    }

    @PostMapping("/create")
    public ApiResponse<String> createAuction(@RequestBody AuctionCreationRequest request) {
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(auctionService.createAuction(request));
        return apiResponse;
    }

    // API Lấy danh sách đấu giá
    @GetMapping
    public ApiResponse<List<Auction>> getAllAuctions() {
        ApiResponse<List<Auction>> response = new ApiResponse<>();
        response.setResult(auctionService.getAllAuctions());
        return response;
    }

    // API Lấy dữ liệu biểu đồ giá theo thời gian thực
    @GetMapping("/{auctionId}/price-chart")
    public ApiResponse<List<BidTransaction>> getPriceChart(@PathVariable String auctionId) {
        ApiResponse<List<BidTransaction>> response = new ApiResponse<>();
        try {
            List<BidTransaction> chartData = auctionService.getPriceChart(auctionId);
            response.setResult(chartData);
        } catch (Exception e) { throw e;}
        return response;
    }

}
