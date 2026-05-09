package org.auctionfx.auctionbidsystemspringbootrework.dto.request;

import jakarta.validation.constraints.NotBlank;


// Request để Admin hủy sản phẩm
public class ItemCancellationRequest {
    @NotBlank(message = "REASON_INVALID")
    private String reason;

    // GETTER VÀ SETTER
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
