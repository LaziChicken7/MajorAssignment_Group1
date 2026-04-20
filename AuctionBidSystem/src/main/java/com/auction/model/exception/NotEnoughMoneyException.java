package com.auction.model.exception;

public class NotEnoughMoneyException extends Exception {
    public NotEnoughMoneyException(String msg) {
        super(msg);
    }
}
