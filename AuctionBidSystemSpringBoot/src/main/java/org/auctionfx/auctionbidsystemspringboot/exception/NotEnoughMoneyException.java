package org.auctionfx.auctionbidsystemspringboot.exception;

public class NotEnoughMoneyException extends Exception {
    public NotEnoughMoneyException(String msg) {
        super(msg);
    }
}
