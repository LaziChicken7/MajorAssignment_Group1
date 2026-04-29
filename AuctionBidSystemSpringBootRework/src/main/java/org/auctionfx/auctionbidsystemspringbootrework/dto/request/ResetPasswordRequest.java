package org.auctionfx.auctionbidsystemspringbootrework.dto.request;

public class ResetPasswordRequest {
    private String userName;
    private String resetToken; // Mã bí mật cấp ở Bước 1 (Verify Info Request)
    private String newPassword;

    // GETTER VÀ SETTER

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
