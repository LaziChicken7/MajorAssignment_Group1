package org.auctionfx.auctionbidsystemspringbootrework.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/uploads")
@Slf4j
public class ImageDisplayController {

    // Thư mục gốc chứa ảnh (Nằm ngoài cùng project)
    private static final String UPLOAD_DIR = "uploads/";

    // Dùng "/**" để bắt MỌI cấp thư mục lồng nhau sau chữ /uploads
    // Ví dụ: /uploads/images/items/uuid1/anh.jpg đều chạy vào hàm này
    @GetMapping("/**")
    public ResponseEntity<byte[]> serveImage(
            HttpServletRequest request,
            @RequestParam(name = "w", required = false) Integer width,
            @RequestParam(name = "h", required = false) Integer height) {

        try {
            // 1. TRÍCH XUẤT ĐƯỜNG DẪN ẢNH ĐỘNG
            // Lấy URL thực tế (VD: /auction/uploads/images/items/uuid/anh.jpg)
            String requestURI = request.getRequestURI();

            // Cắt bỏ phần context-path (nếu có, VD: /auction)
            String contextPath = request.getContextPath();
            String pathWithoutContext = requestURI.substring(contextPath.length());
            // Kết quả: /uploads/images/items/uuid/anh.jpg

            // Cắt bỏ chữ "/uploads/" ở đầu để lấy phần đuôi
            String relativePath = pathWithoutContext.substring("/uploads/".length());
            // Kết quả: images/items/uuid/anh.jpg

            // 2. TÌM FILE TRONG Ổ CỨNG
            Path filePath = Paths.get(UPLOAD_DIR, relativePath).normalize();
            File imgFile = filePath.toFile();

            if (!imgFile.exists()) {
                log.warn("LỖI: Không tìm thấy ảnh tại đường dẫn vật lý: {}", filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }

            byte[] imageBytes;

            // 3. THUẬT TOÁN ÉP SIZE ẢNH
            if (width != null && height != null) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                Thumbnails.of(imgFile)
                        .size(width, height)
                        .outputQuality(0.8) // Nén nhẹ 80%
                        .toOutputStream(os);
                imageBytes = os.toByteArray();
                log.debug("Đã nén ảnh {} xuống {}x{}", imgFile.getName(), width, height);
            } else {
                imageBytes = Files.readAllBytes(filePath);
                log.debug("Đã trả file ảnh gốc sắc nét: {}", imgFile.getName());
            }

            // 4. NHẬN DIỆN KIỂU FILE & TRẢ VỀ
            String mimeType = Files.probeContentType(filePath);
            if (mimeType == null) {
                mimeType = "image/jpeg"; // Đặt mặc định nếu không nhận ra (.jpg)
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(mimeType));
            headers.setCacheControl("max-age=86400, public");

            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("LỖI HỆ THỐNG: Không thể load ảnh", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}