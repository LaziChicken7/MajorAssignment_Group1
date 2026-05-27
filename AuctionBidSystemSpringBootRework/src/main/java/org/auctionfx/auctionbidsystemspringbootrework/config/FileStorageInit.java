package org.auctionfx.auctionbidsystemspringbootrework.config; // Đổi lại package cho đúng

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Component
@Slf4j
public class FileStorageInit implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        log.info("Kiểm tra hệ thống lưu trữ file...");

        // 1. Định nghĩa đường dẫn thư mục nằm ngoài file JAR
        Path uploadDir = Paths.get("uploads/images/avatar/");

        // 2. Nếu bên ngoài chưa có thư mục này (Máy mới tải về) -> Tự động tạo
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
            log.info("Đã tạo thư mục lưu trữ: {}", uploadDir.toAbsolutePath());
        }

        // 3. Đường dẫn file avatar mặc định bên ngoài ổ cứng
        Path defaultAvatarFile = uploadDir.resolve("avatarmacdinh.png");

        // 4. Nếu bên ngoài chưa có ảnh này -> Copy từ bên trong lõi JAR ra ngoài
        if (!Files.exists(defaultAvatarFile)) {
            log.info("Không tìm thấy Avatar mặc định bên ngoài ổ cứng. Đang tiến hành trích xuất từ lõi JAR...");

            // Đọc file từ thư mục src/main/resources/default/
            try (InputStream in = getClass().getResourceAsStream("/default/avatarmacdinh.png")) {
                if (in != null) {
                    Files.copy(in, defaultAvatarFile, StandardCopyOption.REPLACE_EXISTING);
                    log.info("Trích xuất Avatar mặc định thành công!");
                } else {
                    log.error("LỖI: Không tìm thấy file /default/avatarmacdinh.png trong src/main/resources!");
                }
            } catch (Exception e) {
                log.error("Lỗi khi copy avatar mặc định: {}", e.getMessage());
            }
        }
    }
}