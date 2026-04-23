package org.auctionfx.auctionbidsystemspringbootrework.entity.user;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "admins")
public class Admin extends User{
    private String department = "General";

    public String department() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
