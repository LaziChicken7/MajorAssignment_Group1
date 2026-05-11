package org.auctionfx.auctionbidsystemspringbootrework.controller;

import lombok.extern.slf4j.Slf4j;
import org.auctionfx.auctionbidsystemspringbootrework.dto.request.ItemCancellationRequest;
import org.auctionfx.auctionbidsystemspringbootrework.dto.request.ItemCreationRequest;
import org.auctionfx.auctionbidsystemspringbootrework.dto.response.ApiResponse;
import org.auctionfx.auctionbidsystemspringbootrework.entity.item.Item;
import org.auctionfx.auctionbidsystemspringbootrework.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/items")
@Slf4j // Kích hoạt bộ ghi log của Lombok
public class ItemController {

    @Autowired
    private ItemService itemService;

    // Create Item
    @PostMapping("/create")
    public ApiResponse<String> createItem(@RequestBody ItemCreationRequest request) {
        log.info("API CALL: Yêu cầu tạo Item mới từ Seller [{}]", request.getSellerUserName());
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(itemService.createItem(request));
        return apiResponse;
    }

    @GetMapping
    public ApiResponse<List<Item>> getAllItems() {
        log.debug("API CALL: Yêu cầu lấy danh sách toàn bộ Item");
        ApiResponse<List<Item>> apiResponse = new ApiResponse<>();
        apiResponse.setResult(itemService.getAllItems());
        return apiResponse;
    }

    // Endpoint cho Admin hủy sản phẩm
    // Lấy itemId từ path, lý do hủy (reason) từ request body
    @PutMapping("/cancel/{itemId}")
    public ApiResponse<String> cancelItemByAdmin(@PathVariable String itemId, @RequestBody ItemCancellationRequest request) {
        log.warn("API CALL: Admin yêu cầu HỦY Item [{}] với lý do: {}", itemId, request.getReason());
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(itemService.cancelItemByAdmin(itemId, request));
        return apiResponse;
    }

    // API MỚI: UPLOAD ẢNH THEO ITEM ID
    @PostMapping("/{itemId}/upload-images")
    public ResponseEntity<ApiResponse<List<String>>> uploadImagesForItem(
            @PathVariable String itemId,
            @RequestParam("files") MultipartFile[] files) throws IOException {

        log.info("API CALL: Yêu cầu tải lên {} ảnh cho Item [{}]", (files != null ? files.length : 0), itemId);
        ApiResponse<List<String>> response = new ApiResponse<>();

        // Đẩy toàn bộ logic upload file sang Service để Controller gọn gàng
        List<String> uploadedUrls = itemService.uploadImagesForItem(itemId, files);

        response.setCode(1000);
        response.setMessage("Upload success to folder: " + itemId);
        response.setResult(uploadedUrls);

        return ResponseEntity.ok(response);
    }
}