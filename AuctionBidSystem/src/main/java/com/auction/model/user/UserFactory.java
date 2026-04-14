package com.auction.model.user;

import com.auction.model.enums.Role;

public class UserFactory {

    public static User createUser(Role role, String userName, String password, String fullName,
                                  String email, String numberPhone, String citizenId) {
        switch (role) {
            case ADMIN:
                // Admin cần department, mặc định để là "General"
                return new Admin(userName, password, fullName, email, numberPhone, citizenId, Role.ADMIN, "General");

            case SELLER:
                // Seller kế thừa Bidder nên cần address, mặc định để trống
                return new Seller(userName, password, fullName, email, numberPhone, citizenId, "", Role.SELLER);

            case BIDDER:
            default:
                // Bidder cần address
                return new Bidder(userName, password, fullName, email, numberPhone, citizenId, "", Role.BIDDER);
        }
    }
}
