package org.auctionfx.auctionbidsystemspringbootrework.entity.user;

import jakarta.persistence.*;
import org.auctionfx.auctionbidsystemspringbootrework.entity.base.BaseEntity;
import org.auctionfx.auctionbidsystemspringbootrework.enums.Role;

import java.util.concurrent.atomic.AtomicInteger;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public class User extends BaseEntity {

    // KHAI BÁO THUỘC TÍNH
    @Column(unique = true, nullable = false)
    protected String userCode; // THÊM CỘT NÀY ĐỂ LƯU "BID1", "SLR2"...

    @Column(unique = true, nullable = false)
    protected String userName;

    protected String fullName;

    @Column(unique = true)
    protected String email;

    protected String password; // Mã hóa ở Service và đặt điều kiện trong Request

    @Column(unique = true)
    protected String numberPhone;

    @Column(unique = true)
    protected String citizenId;

    @Enumerated(EnumType.STRING) // Lưu chữ SELLER, BIDDER
    protected Role role;

    // THÊM TRƯỜNG NÀY ĐỂ QUẢN LÝ BAN USER
    protected boolean isBanned = false;

    // LẤY VÀ UPDATE THUỘC TÍNH


    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNumberPhone() {
        return numberPhone;
    }

    public void setNumberPhone(String numberPhone) {
        this.numberPhone = numberPhone;
    }

    public String getCitizenId() {
        return citizenId;
    }

    public void setCitizenId(String citizenId) {
        this.citizenId = citizenId;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isBanned() {
        return isBanned;
    }

    public void setBanned(boolean banned) {
        isBanned = banned;
    }
}
