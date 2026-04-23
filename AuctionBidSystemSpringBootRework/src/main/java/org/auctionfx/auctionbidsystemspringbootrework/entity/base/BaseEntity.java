package org.auctionfx.auctionbidsystemspringbootrework.entity.base;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass // RẤT QUAN TRỌNG: Báo cho Spring biết đây là class cha chứa các cột chung
public abstract class BaseEntity {
    // Đánh dấu ID là khóa chính
    @Id
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