package org.auctionfx.auctionbidsystemspringbootrework.exception;

public class AuctionException extends RuntimeException {
    private ErrorCode errorCode;

    public AuctionException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() { return errorCode; }

    public void setErrorCode(ErrorCode errorCode) { this.errorCode = errorCode; }
}
