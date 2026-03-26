package com.auction.model;

public class User extends Entity{
    protected String username;
    protected String password;
    protected String fullname;
    protected String email;
    protected String numberPhone;
    protected String cccd;

    public User(String id, String username, String password, String fullname, String email, String numberPhone, String cccd) {
        super(id);
        this.username = username;
        this.password = password;
        this.fullname = fullname;
        this.email = email;
        this.numberPhone = numberPhone;
        this.cccd = cccd;
    }
}
