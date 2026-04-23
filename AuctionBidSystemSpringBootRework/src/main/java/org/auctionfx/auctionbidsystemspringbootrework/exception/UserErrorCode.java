package org.auctionfx.auctionbidsystemspringbootrework.exception;

public enum UserErrorCode {
    // ===========================================================================
    // REGISTER EXCEPTION
    INVALID_KEY(1001, "Invalid message key"),
    USERNAME_EXISTED(1002, "Username already existed"),
    EMAIL_EXISTED(1003, "Mail already existed"),
    CITIZEN_ID_EXISTED(1004, "Citizen ID already existed"),
    PHONE_NUMBER_EXISTED(1005, "Phone number already existed"),
    // ===========================================================================
    // INVALID EXCEPTION
    USERNAME_INVALID(2001, "Username must be at least 4 characters and not blank"),
    PASSWORD_INVALID(2002, "Password must be at least 8 characters and not blank"),
    // ===========================================================================
    // USER EXCEPTION
    USER_NOT_FOUND(3001, "User not founded"),
    // ===========================================================================
    // ROLE EXCEPTION
    USER_ALREADY_SELLER(4001, "User already seller"),
    USER_CONFLICT_UPGRADE(4999, "Can not upgrade to seller"),
    // ===========================================================================
    UNCATEGORIZED_EXCEPTION(9999, "Something went wrong!"),
    ;

    UserErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    private int code;
    private String message;

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
