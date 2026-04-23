package org.auctionfx.auctionbidsystemspringbootrework.exception;

public class UserException extends RuntimeException {
    private UserErrorCode errorCode;

    public UserException(UserErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public UserErrorCode getErrorCode() { return errorCode; }

    public void setErrorCode(UserErrorCode errorCode) { this.errorCode = errorCode; }
}
