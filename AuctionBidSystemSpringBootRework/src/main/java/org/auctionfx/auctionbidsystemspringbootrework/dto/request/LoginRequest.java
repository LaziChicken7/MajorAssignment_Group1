package org.auctionfx.auctionbidsystemspringbootrework.dto.request;

public class LoginRequest {
    private String userName;
    private String password;

    // GETTER VÀ SETTER

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
