package com.auction.model;

public class Admin extends User{
    private String department;

    public Admin(String id, String username, String password, String fullname, String email, String numberPhone, String cccd, String department) {
        super(id, username, password, fullname, email, numberPhone, cccd);
        this.department = department;
    }

    public String department() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
