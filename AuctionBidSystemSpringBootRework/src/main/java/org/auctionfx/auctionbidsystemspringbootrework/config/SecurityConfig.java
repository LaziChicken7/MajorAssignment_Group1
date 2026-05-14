package org.auctionfx.auctionbidsystemspringbootrework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity // Kích hoạt tùy chỉnh Security của Spring Boot
public class SecurityConfig {

    // 1. Tạo Bean PasswordEncoder dùng chuẩn BCrypt của Spring Security
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 2. Cấu hình phân quyền API (Mở cửa toàn bộ)
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Tắt bảo vệ CSRF (Rất quan trọng: Bắt buộc tắt để JavaFX / Postman gọi được POST/PUT)
                .csrf(csrf -> csrf.disable())

                // Cấu hình phân quyền: Cho phép (permitAll) tất cả các request đi qua mà không cần đăng nhập
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}