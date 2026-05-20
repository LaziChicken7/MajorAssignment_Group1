package org.auctionfx.auctionbidsystemspringbootrework.repository;

import jakarta.persistence.LockModeType;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    // Thủ kho kiểm tra xem các thông tin này đã tồn tại trong Database chưa
    boolean existsByUserName(String userName);
    boolean existsByEmail(String email);
    boolean existsByCitizenId(String citizenId);
    boolean existsByNumberPhone(String numberPhone);

    Optional<User> findById(String id);

    User findByUserName(String userName); // Thêm hàm này để tìm User

    // --- CÁC HÀM XỬ LÝ NÂNG CẤP ROLE ---

    // 1. TÌM SỐ THỨ TỰ LỚN NHẤT CỦA TOÀN BỘ HỆ THỐNG
    @Query(value = "SELECT MAX(CAST(SUBSTRING(user_code, 4) AS UNSIGNED)) FROM users", nativeQuery = true)
    Integer findMaxUserCodeNumber(); // Không cần truyền tham số prefix vào nữa

    // 2. CẬP NHẬT ROLE VÀ userCode CÙNG LÚC
    @Modifying
    @Query(value = "UPDATE users SET role = 'SELLER', user_code = :newCode WHERE id = :userId", nativeQuery = true)
    void upgradeToSellerAndUpdateCode(@Param("userId") String userId, @Param("newCode") String newCode);

    // 3. THÊM VÀO BẢNG SELLERS
    @Modifying
    @Query(value = "INSERT INTO sellers (id, rating) VALUES (:userId, 0.0)", nativeQuery = true)
    void insertIntoSellersTable(@Param("userId") String userId);

    // 4. Tìm người dùng theo Tên đăng nhập hoặc Họ tên
    @Query("SELECT u FROM User u WHERE LOWER(u.userName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<User> searchUsers(@org.springframework.data.repository.query.Param("keyword") String keyword);
}
