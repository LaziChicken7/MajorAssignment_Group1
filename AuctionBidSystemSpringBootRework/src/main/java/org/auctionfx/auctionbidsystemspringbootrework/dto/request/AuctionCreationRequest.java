package org.auctionfx.auctionbidsystemspringbootrework.dto.request;

import java.time.LocalDateTime;

public class AuctionCreationRequest {
    private String itemId;
    private LocalDateTime endTime;  // Chỉ cần gửi thời gian kết thúc, thời gian bắt đầu sẽ tự lấy lúc tạo

    // GETTER VÀ SETTER

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}
