package org.auctionfx.auctionbidsystemspringbootrework.repository;

import org.auctionfx.auctionbidsystemspringbootrework.entity.chat.Connection;
import org.auctionfx.auctionbidsystemspringbootrework.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConnectionRepository extends JpaRepository<Connection, String> {

    // Tìm danh sách bạn bè ĐÃ KẾT BẠN (status = ACCEPTED)
    @Query("SELECT c FROM Connection c WHERE (c.sender = :user OR c.receiver = :user) AND c.status = 'ACCEPTED'")
    List<Connection> findAcceptedConnectionsByUser(@Param("user") User user);

    // Kiểm tra xem 2 người đã kết bạn chưa
    @Query("SELECT c FROM Connection c WHERE (c.sender = :u1 AND c.receiver = :u2) OR (c.sender = :u2 AND c.receiver = :u1)")
    Connection findExistingConnection(@Param("u1") User u1, @Param("u2") User u2);
}