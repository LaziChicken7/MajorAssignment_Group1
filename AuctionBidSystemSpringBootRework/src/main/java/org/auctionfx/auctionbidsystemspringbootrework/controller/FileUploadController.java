package org.auctionfx.auctionbidsystemspringbootrework.controller;

import lombok.extern.slf4j.Slf4j;
import org.auctionfx.auctionbidsystemspringbootrework.dto.response.ApiResponse;
import org.auctionfx.auctionbidsystemspringbootrework.service.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

//  ***** FileUploadController *****
//    Mục đích: Cho phép client upload ảnh lên server để lưu trữ.
//    Sử dụng cho:
//   - Ảnh sản phẩm (Item images): Upload nhiều ảnh cho một sản phẩm đấu giá
//   - Ảnh đại diện người dùng (User avatar): Upload ảnh profile cho user
//   Cách hoạt động:
//   1. Client gửi file qua HTTP POST với form-data
//   2. Server lưu file vào thư mục "uploads/" với tên ngẫu nhiên (UUID)
//   3. Trả về URL truy cập file (vd: "/uploads/abc123.png")
//   4. Client sử dụng URL này để lưu vào database (Item.imageUrls hoặc User.avatarUrl)
//   5. WebConfig sẽ map URL "/uploads/**" đến thư mục vật lý "uploads/" để phục vụ file tĩnh
//   Bảo mật: File được đổi tên ngẫu nhiên để tránh conflict và tăng bảo mật.

@RestController
@RequestMapping("/files")
@Slf4j // Kích hoạt bộ ghi log của Lombok
public class FileUploadController {

    @Autowired
    private FileUploadService fileUploadService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<String>> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        log.info("API CALL: Yêu cầu upload 1 file đơn lẻ");
        ApiResponse<String> response = new ApiResponse<>();

        // Gọi sang Service để xử lý file
        String fileUrl = fileUploadService.uploadFile(file);

        // Trả về response thành công
        response.setCode(2000);
        response.setMessage("Upload successful");
        response.setResult(fileUrl);

        return ResponseEntity.ok(response);
    }

    // API uoload nhiều file cùng lúc (dùng cho ảnh spham)
    @PostMapping("/upload-multiple")
    public ResponseEntity<ApiResponse<List<String>>> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) throws IOException {
        log.info("API CALL: Yêu cầu upload nhiều file ({} file)", (files != null ? files.length : 0));
        ApiResponse<List<String>> response = new ApiResponse<>();

        // Gọi sang Service để xử lý mảng file
        List<String> fileUrls = fileUploadService.uploadMultipleFiles(files);

        // Trả về response với danh sách URLs
        response.setCode(2000);
        response.setMessage("Upload " + fileUrls.size() + " files successful");
        response.setResult(fileUrls);

        return ResponseEntity.ok(response);
    }
}