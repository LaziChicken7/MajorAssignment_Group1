package org.auctionfx.auctionbidsystemspringbootrework.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Path;
import java.nio.file.Paths;


//  * WebConfig - Cấu hình phục vụ file tĩnh
//  * Mục đích: Cho phép truy cập các file ảnh đã upload qua URL
//  * Ví dụ:
//  * - File lưu: uploads/abc123.png
//  * - URL truy cập: http://localhost:8080/uploads/abc123.png
//  * Sử dụng cho:
//  * - Hiển thị ảnh sản phẩm trong Item
//  * - Hiển thị avatar user


@Configuration
public class WebConfig implements WebMvcConfigurer {

    //  * Cấu hình Resource Handler cho thư mục uploads
    //  * Khi client request URL bắt đầu bằng "/uploads/",
    //  * Spring Boot sẽ tìm file tương ứng trong thư mục "uploads/" trên server
    
//    @Override
//    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//        // Đường dẫn đến thư mục uploads
//        Path uploadDir = Paths.get("uploads");
//        String uploadPath = uploadDir.toFile().getAbsolutePath();
//
//        // Map URL pattern "/uploads/**" đến thư mục vật lý
//        registry.addResourceHandler("/uploads/**")
//                .addResourceLocations("file:" + uploadPath + "/");
//    }
}
