package org.auctionfx.auctionbidsystemspringbootrework.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.NoArgsConstructor;


// Request để Admin hủy sản phẩm
@NoArgsConstructor
public class ItemCancellationRequest {
    @NotBlank(message = "REASON_INVALID")
    private String reason;

    // GETTER VÀ SETTER
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
