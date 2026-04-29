package org.auctionfx.auctionbidsystemspringbootrework.dto.request;

public class VerifyInfoRequest {
    private String userName;
    private String email;
    private String citizenId;

    // GETTER VÀ SETTER

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCitizenId() {
        return citizenId;
    }

    public void setCitizenId(String citizenId) {
        this.citizenId = citizenId;
    }
}
