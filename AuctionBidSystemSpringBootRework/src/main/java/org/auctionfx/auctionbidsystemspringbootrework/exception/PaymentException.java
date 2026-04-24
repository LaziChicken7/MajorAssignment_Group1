package org.auctionfx.auctionbidsystemspringbootrework.exception;

public class PaymentException extends RuntimeException {
    private ErrorCode errorCode;

    public PaymentException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() { return errorCode; }

    public void setErrorCode(ErrorCode errorCode) { this.errorCode = errorCode; }
}
