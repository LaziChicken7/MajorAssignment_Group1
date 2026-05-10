package org.auctionfx.auctionbidsystemspringbootrework.controller;

import org.auctionfx.auctionbidsystemspringbootrework.dto.request.ItemCancellationRequest;
import org.auctionfx.auctionbidsystemspringbootrework.dto.request.ItemCreationRequest;
import org.auctionfx.auctionbidsystemspringbootrework.dto.response.ApiResponse;
import org.auctionfx.auctionbidsystemspringbootrework.entity.item.Item;
import org.auctionfx.auctionbidsystemspringbootrework.service.ItemService;
import org.auctionfx.auctionbidsystemspringbootrework.entity.item.Item;
import org.auctionfx.auctionbidsystemspringbootrework.repository.ItemRepository;
import org.auctionfx.auctionbidsystemspringbootrework.exception.ItemException;
import org.auctionfx.auctionbidsystemspringbootrework.exception.ErrorCode;
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
    @Autowired private ItemService itemService;

    @Autowired private ItemRepository itemRepository; // Thêm dòng này

    // Thư mục lưu trữ file upload trên server (tương đối từ root project)
    private static final String UPLOAD_DIR = "uploads/images/items/";

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

    // API MỚI: UPLOAD ẢNH THEO ITEM ID
    @PostMapping("/{itemId}/upload-images")
    public ResponseEntity<ApiResponse<List<String>>> uploadImagesForItem(
            @PathVariable String itemId,
            @RequestParam("files") MultipartFile[] files) throws IOException {

        ApiResponse<List<String>> response = new ApiResponse<>();

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemException(ErrorCode.ITEM_NOT_FOUND));

        if (files == null || files.length == 0) {
            throw new IOException("No files provided");
        }

        // 2. Thư mục vật lý sẽ tạo ra: uploads/images/items/{itemId}/
        String itemDirPath = UPLOAD_DIR + itemId + "/";
        File uploadDir = new File(itemDirPath);
        if (!uploadDir.exists() && !uploadDir.mkdirs()) {
            throw new IOException("Could not create directory: " + itemDirPath);
        }

        List<String> fileUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String originalFileName = file.getOriginalFilename();
            String fileExtension = originalFileName != null && originalFileName.contains(".")
                    ? originalFileName.substring(originalFileName.lastIndexOf(".")) : "";

            String newFileName = UUID.randomUUID().toString() + fileExtension;
            Path filePath = Paths.get(itemDirPath + newFileName);
            Files.write(filePath, file.getBytes());

            // 3. Đường dẫn lưu vào Database sẽ là: /uploads/images/items/{itemId}/{newFileName}
            // (Thêm dấu "/" ở đầu để chuẩn định dạng URL web)
            String fileUrl = "/" + UPLOAD_DIR + itemId + "/" + newFileName;
            fileUrls.add(fileUrl);
        }

        List<String> currentImages = item.getImageUrls();
        if (currentImages == null) currentImages = new ArrayList<>();
        currentImages.addAll(fileUrls);
        item.setImageUrls(currentImages);
        itemRepository.save(item);

        response.setCode(1000);
        response.setMessage("Upload success to folder: " + itemId);
        response.setResult(fileUrls);
        return ResponseEntity.ok(response);
    }
}