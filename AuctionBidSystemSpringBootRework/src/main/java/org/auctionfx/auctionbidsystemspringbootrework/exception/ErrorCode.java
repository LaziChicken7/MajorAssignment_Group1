package org.auctionfx.auctionbidsystemspringbootrework.exception;

public enum ErrorCode {
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
    USER_INVALID(3002, "User invalid or do not have bid permission"),
    // ===========================================================================
    // ROLE EXCEPTION
    USER_ALREADY_SELLER(4001, "User already seller"),
    USER_CONFLICT_UPGRADE(4999, "Can not upgrade to seller"),
    // ===========================================================================
    // PAYMENT EXCEPTION
    NOT_ENOUGH_MONEY_ON_WALLET(5001, "Not enough money on wallet"),
    NOT_ENOUGH_MONEY_IN_FROZEN(5002, "Not enough money in Frozen"),
    CONDITION_ACCEPT_PAYMENT_INVALID(5003, "Condition accept payment invalid"),
    CONDITION_DECLINE_PAYMENT_INVALID(5004, "Condition decline payment invalid"),
    CONDITION_CANCEL_AUCTION_INVALID(5005, "Condition cancel auction invalid"),
    DEPOSIT_MONEY_INVALID(5006, "Deposit money must be more than zero"),
    WITHDRAW_MONEY_INVALID(5007, "Withdraw money must be more than zero"),
    // ===========================================================================
    // AUCTION EXCEPTION
    AUCTION_NOT_FOUND(6001, "Auction not founded"),
    AUCTION_NOT_RUNNING(6002, "Auction not in running status"),
    AUCTION_BIDDER_INVALID(6003, "Bidder can not bid own product"),
    // ===========================================================================
    // ITEM CREATE EXCEPTION
    SELLER_INVALID(7001, "User is not seller, can not create item"),
    ITEM_INVALID(7002, "Invalid item type"),
    ITEM_NOT_FOUND(7003, "Item not found"),
    // ===========================================================================
    // RUNTIME EXCEPTION
    RUNTIME_EXCEPTION(89999, "Runtime exception"),
    // ===========================================================================
    UNCATEGORIZED_EXCEPTION(99999, "Something went wrong!"),
    ;

    ErrorCode(int code, String message) {
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
