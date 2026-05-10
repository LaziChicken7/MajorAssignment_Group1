package org.auctionfx.auctionbidsystemspringbootrework.controller;

import org.auctionfx.auctionbidsystemspringbootrework.dto.request.ItemCreationRequest;
import org.auctionfx.auctionbidsystemspringbootrework.dto.response.ApiResponse;
import org.auctionfx.auctionbidsystemspringbootrework.entity.item.Item;
import org.auctionfx.auctionbidsystemspringbootrework.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/items")
public class ItemController {
    @Autowired
    private ItemService itemService;

    // Create Item
    @PostMapping("/create")
    public ApiResponse<String> createItem(@RequestBody ItemCreationRequest request) {
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(itemService.createItem(request));
        return apiResponse;
    }

    @GetMapping
    public ApiResponse<List<Item>> getAllItems() {
        ApiResponse<List<Item>> apiResponse = new ApiResponse<>();
        apiResponse.setResult(itemService.getAllItems());
        return apiResponse;
    }

    // Endpoint cho Admin hủy sản phẩm
    // Lấy itemId từ path, lý do hủy (reason) từ request body
    @PutMapping("/cancel/{itemId}")
    public ApiResponse<String> cancelItemByAdmin(
            @PathVariable String itemId,
            @RequestBody org.auctionfx.auctionbidsystemspringbootrework.dto.request.ItemCancellationRequest request) {
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(itemService.cancelItemByAdmin(itemId, request));
        return apiResponse;
    }
}