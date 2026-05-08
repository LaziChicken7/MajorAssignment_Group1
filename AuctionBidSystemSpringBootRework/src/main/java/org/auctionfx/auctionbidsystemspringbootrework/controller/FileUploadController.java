package org.auctionfx.auctionbidsystemspringbootrework.controller;

import org.auctionfx.auctionbidsystemspringbootrework.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

    // Controller để xử lý upload file - dùng cho cả Item(ảnh sản phẩm) và User(avatar)
@RestController
@RequestMapping("/files")
public class FileUploadController {

    // Thư mục lưu trữ ảnh gốc
    private static final String UPLOAD_DIR = "uploads/";

    // API Upload file, phục vụ chung cho cả Item(sản phẩm) và User(avatar)
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<String>> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        ApiResponse<String> response = new ApiResponse<>();

        if (file == null || file.isEmpty()) {
            throw new IOException("No file uploaded or file is empty");
        }

        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists() && !uploadDir.mkdirs()) {
            throw new IOException("Could not create upload directory");
        }

        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String newFileName = UUID.randomUUID().toString() + fileExtension;

        Path filePath = Paths.get(UPLOAD_DIR + newFileName);
        
        // Lệnh này nếu lỗi (ví dụ đầy ổ cứng) sẽ tự động ném IOException lên cho Handler
        Files.write(filePath, file.getBytes());

        // all oke trả về mã 2000
        String fileUrl = "/uploads/" + newFileName;
        response.setCode(2000);
        response.setMessage("Upload successful");
        response.setResult(fileUrl);
        
        return ResponseEntity.ok(response);
    }
}