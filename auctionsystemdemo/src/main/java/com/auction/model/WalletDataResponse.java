package com.auction.model;
import java.util.List;

public class WalletDataResponse {
    public String bankAccountNumber; // THÊM DÒNG NÀY
    public double moneyOnWallet;
    public double moneyinFrozen;
    public List<TransactionHistoryResponse> successTransaction;
    public List<TransactionHistoryResponse> failedTransaction;

    public String getBankAccountNumber() {
        return bankAccountNumber;
    }

    public void setBankAccountNumber(String bankAccountNumber) {
        this.bankAccountNumber = bankAccountNumber;
    }

    public double getMoneyOnWallet() {
        return moneyOnWallet;
    }

    public void setMoneyOnWallet(double moneyOnWallet) {
        this.moneyOnWallet = moneyOnWallet;
    }

    public double getMoneyinFrozen() {
        return moneyinFrozen;
    }

    public void setMoneyinFrozen(double moneyinFrozen) {
        this.moneyinFrozen = moneyinFrozen;
    }

    public List<TransactionHistoryResponse> getSuccessTransaction() {
        return successTransaction;
    }

    public void setSuccessTransaction(List<TransactionHistoryResponse> successTransaction) {
        this.successTransaction = successTransaction;
    }

    public List<TransactionHistoryResponse> getFailedTransaction() {
        return failedTransaction;
    }

    public void setFailedTransaction(List<TransactionHistoryResponse> failedTransaction) {
        this.failedTransaction = failedTransaction;
    }
}