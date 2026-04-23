package org.auctionfx.auctionbidsystemspringbootrework.repository;

import org.auctionfx.auctionbidsystemspringbootrework.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    // Thủ kho kiểm tra xem các thông tin này đã tồn tại trong Database chưa
    boolean existsByUserName(String userName);
    boolean existsByEmail(String email);
    boolean existsByCitizenId(String citizenId);
    boolean existsByNumberPhone(String numberPhone);

    // THÊM DÒNG NÀY:
    // Cắt 3 chữ cái đầu ("USR"), biến phần còn lại thành số nguyên (int) và tìm số Max
    @Query("SELECT MAX(CAST(SUBSTRING(u.id, 4) AS int)) FROM User u")
    Integer findMaxIdNumber();
}
