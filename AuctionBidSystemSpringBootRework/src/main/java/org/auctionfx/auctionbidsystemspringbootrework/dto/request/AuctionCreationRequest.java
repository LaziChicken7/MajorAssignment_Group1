package org.auctionfx.auctionbidsystemspringbootrework.dto.request;

import java.time.LocalDateTime;

public class AuctionCreationRequest {
    private String itemId;
    private LocalDateTime startTime; // THÊM DÒNG NÀY
    private LocalDateTime endTime;

    // GETTER VÀ SETTER

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}
