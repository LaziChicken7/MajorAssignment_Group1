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
    REASON_INVALID(2003, "Reason must not be blank"),
    // ===========================================================================
    // LOGIN EXCEPTION
    USERNAME_NOT_FOUND(3001, "Username not found"),
    PASSWORD_NOT_MATCH(3002, "Password not match"),
    // ===========================================================================
    // USER EXCEPTION
    USER_NOT_FOUND(4001, "User not founded"),
    USER_INVALID(4002, "User invalid or do not have bid permission"),
    INVALID_RESET_TOKEN(4003, "The verification code is invalid or has expired!"),
    USER_INFO_NOT_MATCH(4004, "Username and Email do not match!"),
    BAN_USER_INVALID(4005, "Can not ban Admin"),
    USER_BANNED(4006, "User banned! Please contact to Admin!"),
    // ===========================================================================
    // ROLE EXCEPTION
    USER_ALREADY_SELLER(5001, "User already seller"),
    USER_CONFLICT_UPGRADE(5999, "Can not upgrade to seller"),
    // ===========================================================================
    // PAYMENT EXCEPTION
    NOT_ENOUGH_MONEY_ON_WALLET(6001, "Not enough money on wallet"),
    NOT_ENOUGH_MONEY_IN_FROZEN(6002, "Not enough money in Frozen"),
    CONDITION_ACCEPT_PAYMENT_INVALID(6003, "Condition accept payment invalid"),
    CONDITION_DECLINE_PAYMENT_INVALID(6004, "Condition decline payment invalid"),
    CONDITION_CANCEL_AUCTION_INVALID(6005, "Condition cancel auction invalid"),
    DEPOSIT_MONEY_INVALID(6006, "Deposit money must be more than zero"),
    WITHDRAW_MONEY_INVALID(6007, "Withdraw money must be more than zero"),
    // ===========================================================================
    // AUCTION EXCEPTION
    AUCTION_NOT_FOUND(7001, "Auction not founded"),
    AUCTION_NOT_RUNNING(7002, "Auction not in running status"),
    AUCTION_BIDDER_INVALID(7003, "Bidder can not bid own product"),
    BARCHART_CONNECT_FAILURE(7004, "Failed to extract chart data"),
    BID_AMOUNT_INVALID(7005, "Money must be higher than now"),
    // ===========================================================================
    // ITEM CREATE EXCEPTION
    SELLER_INVALID(8001, "User is not seller, can not create item"),
    ITEM_INVALID(8002, "Invalid item type"),
    ITEM_NOT_FOUND(8003, "Item not found"),
    // ===========================================================================
    // NOTIFICATION EXCEPTION
    NOTIFICATION_NOT_FOUND(9001, "Notification not found"),
    NOTIFICATION_DELETE_INVALID(9002, "Can not delete verification notification. User must be choose Accept or Decline"),
    NOTIFICATION_ACCEPT_PAYMENT_INVALID(9003, "This notification not required to Accept payment"),
    NOTIFICATION_DECLINE_PAYMENT_INVALID(9004, "This notification not required to Decline payment"),
    // ===========================================================================
    // REVIEW EXCEPTION
    REVIEW_EXISTED(10001, "User has been reviewed seller before"),
    RATING_INVALID(10002, "Invalid star, star must be between 0 and 5"),
    // ===========================================================================
    // RUNTIME EXCEPTION
    RUNTIME_EXCEPTION(89999, "Runtime exception"),
    // ===========================================================================
    UNCATEGORIZED_EXCEPTION(99999, "Something went wrong!");

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
