package org.auctionfx.auctionbidsystemspringbootrework.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j // Kích hoạt bộ ghi log của Lombok
public class FileUploadService {

    // Thư mục lưu trữ file upload trên server (tương đối từ root project)
    private static final String UPLOAD_DIR = "uploads/";

    public String uploadFile(MultipartFile file) throws IOException {
        log.info("SERVICE: Bắt đầu xử lý lưu file đơn lẻ vật lý");

        // check file null or empty
        if (file == null || file.isEmpty()) {
            log.error("Lỗi Upload: File trống hoặc không tồn tại");
            throw new IOException("No file uploaded or file is empty");
        }

        // tạo uploads directory nếu chưa có
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists() && !uploadDir.mkdirs()) {
            log.error("LỖI HỆ THỐNG: Không thể tạo thư mục [{}]", UPLOAD_DIR);
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

        log.debug("Đang ghi file vào disk: {}", newFileName);
        // ghi file vào disk, lỗi thì tự động GlobalExceptionHandler bắt -> getCode=500
        Files.write(filePath, file.getBytes());

        // Tạo URL truy cập file (sẽ được WebConfig map đến thư mục uploads)
        String fileUrl = "/uploads/" + newFileName;

        log.info("Upload thành công. Đường dẫn: {}", fileUrl);
        return fileUrl;
    }

    public List<String> uploadMultipleFiles(MultipartFile[] files) throws IOException {
        log.info("SERVICE: Bắt đầu xử lý lưu nhiều file vật lý");
        List<String> fileUrls = new ArrayList<>();

        // Kiểm tra có file nào được gửi không
        if (files == null || files.length == 0) {
            log.error("Lỗi Upload: Mảng files trống hoặc bị Null");
            throw new IOException("No files provided");
        }

        // Tạo thư mục upload nếu chưa tồn tại
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists() && !uploadDir.mkdirs()) {
            log.error("LỖI HỆ THỐNG: Không thể tạo thư mục [{}]", UPLOAD_DIR);
            throw new IOException("Could not create upload directory");
        }

        // Xử lý từng file trong mảng
        for (MultipartFile file : files) {
            // Bỏ qua file rỗng
            if (file.isEmpty()) {
                log.warn("Cảnh báo: Bỏ qua 1 file rỗng trong mảng upload");
                continue;
            }

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

        log.info("Hoàn tất xử lý lưu thành công {} file.", fileUrls.size());
        return fileUrls;
    }
}