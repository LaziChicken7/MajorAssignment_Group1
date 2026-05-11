package org.auctionfx.auctionbidsystemspringbootrework.controller;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j // Kích hoạt bộ ghi log của Lombok
public class AuctionController {
    @Autowired
    private AuctionService auctionService;

    @PostMapping("/{auctionId}/place-bid")
    public ApiResponse<String> placeBid(@PathVariable String auctionId, @RequestBody PlaceBidRequest request) {
        log.info("API CALL: User [{}] yêu cầu đặt giá cho phiên đấu giá [{}]", request.getBidderUserName(), auctionId);
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(auctionService.placeBid(auctionId, request.getBidderUserName(), request.getBidAmount()));
        return apiResponse;
    }

    @PutMapping("/{auctionId}/start")
    public ApiResponse<String> startAuction(@PathVariable String auctionId) {
        log.info("API CALL: Yêu cầu bắt đầu phiên đấu giá [{}]", auctionId);
        auctionService.startAuction(auctionId);
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult("Auction started successfully!");
        return apiResponse;
    }

    @PutMapping("/{auctionId}/close")
    public ApiResponse<String> closeAuction(@PathVariable String auctionId) {
        log.info("API CALL: Yêu cầu đóng phiên đấu giá [{}]", auctionId);
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(auctionService.closeAuction(auctionId));
        return apiResponse;
    }

    @PutMapping("/{auctionId}/accept-payment")
    public ApiResponse<String> acceptPayment(@PathVariable String auctionId) {
        log.info("API CALL: Yêu cầu chấp nhận thanh toán cho phiên đấu giá [{}]", auctionId);
        auctionService.acceptPayment(auctionId);
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult("Payment accepted and transferred successfully!");
        return apiResponse;
    }

    @PutMapping("/{auctionId}/decline-payment")
    public ApiResponse<String> declinePayment(@PathVariable String auctionId) {
        log.info("API CALL: Yêu cầu từ chối thanh toán cho phiên đấu giá [{}]", auctionId);
        auctionService.declinePayment(auctionId);
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult("Payment declined and money refunded successfully!");
        return apiResponse;
    }

    @PutMapping("/{auctionId}/cancel")
    public ApiResponse<String> cancelAuction(@PathVariable String auctionId) {
        log.warn("API CALL: Yêu cầu HỦY phiên đấu giá [{}]", auctionId);
        auctionService.cancelAuction(auctionId);
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult("Auction cancelled successfully!");
        return apiResponse;
    }

    @PostMapping("/create")
    public ApiResponse<String> createAuction(@RequestBody AuctionCreationRequest request) {
        log.info("API CALL: Yêu cầu tạo phiên đấu giá mới cho sản phẩm [{}]", request.getItemId());
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(auctionService.createAuction(request));
        return apiResponse;
    }

    // API Lấy danh sách đấu giá
    @GetMapping
    public ApiResponse<List<Auction>> getAllAuctions() {
        log.debug("API CALL: Yêu cầu lấy danh sách toàn bộ phiên đấu giá");
        ApiResponse<List<Auction>> response = new ApiResponse<>();
        response.setResult(auctionService.getAllAuctions());
        return response;
    }

    // API Lấy dữ liệu biểu đồ giá theo thời gian thực
    @GetMapping("/{auctionId}/price-chart")
    public ApiResponse<List<BidTransaction>> getPriceChart(@PathVariable String auctionId) {
        log.debug("API CALL: Yêu cầu lấy biểu đồ giá cho phiên đấu giá [{}]", auctionId);
        ApiResponse<List<BidTransaction>> response = new ApiResponse<>();
        response.setResult(auctionService.getPriceChart(auctionId));
        return response;
    }

}