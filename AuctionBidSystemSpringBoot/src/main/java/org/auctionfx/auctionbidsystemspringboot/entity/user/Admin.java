package org.auctionfx.auctionbidsystemspringboot.entity.user;


import org.auctionfx.auctionbidsystemspringboot.enums.Role;

public class Admin extends User{
    private String department;

    public Admin(String userName, String password, String fullName, String email, String numberPhone, String citizenId, Role role, String department) {
        super(userName, password, fullName, email, numberPhone, citizenId, role);
        this.department = department;
    }

    public String department() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
