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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


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
public class FileUploadController {

    // Thư mục lưu trữ file upload trên server (tương đối từ root project)
    private static final String UPLOAD_DIR = "uploads/";

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<String>> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        ApiResponse<String> response = new ApiResponse<>();

        // check file null or empty
        if (file == null || file.isEmpty()) {
            throw new IOException("No file uploaded or file is empty");
        }

        // tạo uploads directory nếu chưa có
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists() && !uploadDir.mkdirs()) {
            throw new IOException("Could not create upload directory");
        }

        // lấy tên file gốc + phần mở rộng
        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        // tạo tên mới -> bảo mật + tránh lặp tên file
        String newFileName = UUID.randomUUID().toString() + fileExtension;

        // Đường dẫn đầy đủ đến file sẽ lưu
        Path filePath = Paths.get(UPLOAD_DIR + newFileName);

        // ghi file vào disk, lỗi thì tự động GlobalExceptionHandler bắt -> getCode=500
        Files.write(filePath, file.getBytes());

        // Tạo URL truy cập file (sẽ được WebConfig map đến thư mục uploads)
        String fileUrl = "/uploads/" + newFileName;

        // Trả về response thành công
        response.setCode(2000);
        response.setMessage("Upload successful");
        response.setResult(fileUrl);

        return ResponseEntity.ok(response);
    }

    
    // API uoload nhiều file cùng lúc (dùng cho ảnh spham)
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