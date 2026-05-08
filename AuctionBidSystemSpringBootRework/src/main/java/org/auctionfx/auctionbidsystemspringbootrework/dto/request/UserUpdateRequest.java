package org.auctionfx.auctionbidsystemspringbootrework.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Nơi hứng cục JSON từ Postman
public class UserUpdateRequest {
    @Size(min = 8, message = "PASSWORD_INVALID")
    private String password;

    @NotBlank(message = "Name must not blank")
    private String fullName;

    private String email;
    private String numberPhone;
    private String avatarUrl;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNumberPhone() {
        return numberPhone;
    }

    public void setNumberPhone(String numberPhone) {
        this.numberPhone = numberPhone;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
