package org.auctionfx.auctionbidsystemspringboot.entity.user;


import org.auctionfx.auctionbidsystemspringboot.enums.Role;

public class UserFactory {

    public static User createUser(Role role, String userName, String password, String fullName,
                                  String email, String numberPhone, String citizenId) {
        return switch (role) {
            case ADMIN ->
                // Admin cần department, mặc định để là "General"
                    new Admin(userName, password, fullName, email, numberPhone, citizenId, Role.ADMIN, "General");
            case SELLER ->
                // Seller kế thừa Bidder nên cần address, mặc định để trống
                    new Seller(userName, password, fullName, email, numberPhone, citizenId, "", Role.SELLER);
            default ->
                // Bidder cần address
                    new Bidder(userName, password, fullName, email, numberPhone, citizenId, "", Role.BIDDER);
        };
    }
}
