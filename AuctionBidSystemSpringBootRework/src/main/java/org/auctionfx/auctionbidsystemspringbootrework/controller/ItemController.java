package org.auctionfx.auctionbidsystemspringbootrework.controller;

import org.auctionfx.auctionbidsystemspringbootrework.dto.request.ItemCancellationRequest;
import org.auctionfx.auctionbidsystemspringbootrework.dto.request.ItemCreationRequest;
import org.auctionfx.auctionbidsystemspringbootrework.dto.response.ApiResponse;
import org.auctionfx.auctionbidsystemspringbootrework.entity.item.Item;
import org.auctionfx.auctionbidsystemspringbootrework.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/items")
public class ItemController {
    @Autowired
    private ItemService itemService;

    // Thư mục lưu trữ file upload trên server (tương đối từ root project)
    private static final String UPLOAD_DIR = "uploads/";

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
    public ApiResponse<String> cancelItemByAdmin(@PathVariable String itemId, @RequestBody ItemCancellationRequest request) {
        ApiResponse<String> apiResponse = new ApiResponse<>();
        apiResponse.setResult(itemService.cancelItemByAdmin(itemId, request));
        return apiResponse;
    }

    // API upload nhiều file cùng lúc (dùng cho ảnh sản phẩm)
    @PostMapping("/upload-multiple")
    public ResponseEntity<ApiResponse<List<String>>> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) throws IOException {
        ApiResponse<List<String>> response = new ApiResponse<>();
        List<String> fileUrls = new ArrayList<>();

        // Kiểm tra có file nào được gửi không
        if (files == null || files.length == 0) {
            throw new IOException("No files provided");
        }

        // Tạo thư mục upload nếu chưa tồn tại
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists() && !uploadDir.mkdirs()) {
            throw new IOException("Could not create upload directory");
        }

        // Xử lý từng file trong mảng
        for (MultipartFile file : files) {
            // Bỏ qua file rỗng
            if (file.isEmpty()) continue;

            // Lấy tên file gốc và phần mở rộng
            String originalFileName = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }

            // Tạo tên file mới ngẫu nhiên
            String newFileName = UUID.randomUUID().toString() + fileExtension;

            // Đường dẫn đầy đủ
            Path filePath = Paths.get(UPLOAD_DIR + newFileName);

            // Ghi file
            Files.write(filePath, file.getBytes());

            // Tạo URL và thêm vào danh sách
            String fileUrl = "/uploads/" + newFileName;
            fileUrls.add(fileUrl);
        }

        // Trả về response với danh sách URLs
        response.setCode(2000);
        response.setMessage("Upload " + fileUrls.size() + " files successful");
        response.setResult(fileUrls);
        return ResponseEntity.ok(response);
    }
}