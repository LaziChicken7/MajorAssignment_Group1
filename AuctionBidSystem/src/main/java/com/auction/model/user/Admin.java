package com.auction.model.user;

public class Admin extends User{
    private String department;

    public Admin(String userName, String password, String fullName, String email, String numberPhone, String citizenId, String department) {
        super(userName, password, fullName, email, numberPhone, citizenId);
        this.department = department;
    }

    public String department() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
