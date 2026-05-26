package org.auctionfx.auctionbidsystemspringbootrework.repository;

import org.auctionfx.auctionbidsystemspringbootrework.entity.notification.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, String> {
    // Lấy danh sách thông báo của 1 user, sắp xếp mới nhất lên đầu
    List<Notification> findByUserUserNameOrderByCreatedAtDesc(String userName);
    
    List<Notification> findByTitle(String title);
}
