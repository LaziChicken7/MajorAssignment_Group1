package org.auctionfx.auctionbidsystemspringbootrework.entity.base;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass // RẤT QUAN TRỌNG: Báo cho Spring biết đây là class cha chứa các cột chung
public abstract class BaseEntity {
    // Đánh dấu ID là khóa chính
    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Trả lại quyền sinh ID ngầm cho Spring Boot
    protected String id;

    // GETTER
    public String getId() {
        return id;
    }
    // SETTER
    public void setId(String id) {
        this.id = id;
    }

}