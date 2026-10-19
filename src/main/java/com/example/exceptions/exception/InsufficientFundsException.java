package com.example.exceptions.exception;

import java.math.BigDecimal;

/**
 * Thrown when a withdrawal or transfer would take an account below zero.
 *
 * <p>CHECKED (extends Exception): this is an expected business outcome that callers
 * should consciously decide how to handle (show a message, offer an overdraft, retry
 * with a smaller amount). The compiler forces every caller to catch it or declare
 * {@code throws InsufficientFundsException}, so it can never be forgotten.
 */
public class InsufficientFundsException extends Exception {

    private final String accountId;
    private final BigDecimal balance;
    private final BigDecimal requestedAmount;

    public InsufficientFundsException(String accountId, BigDecimal balance, BigDecimal requestedAmount) {
        // The message is safe to show to the client: it does not reveal the balance.
        super("Account " + accountId + " has insufficient funds for an amount of " + requestedAmount);
        this.accountId = accountId;
        this.balance = balance;
        this.requestedAmount = requestedAmount;
    }

    public String getAccountId() {
        return accountId;
    }

    /** Available for logging/debugging; deliberately NOT included in the API message. */
    public BigDecimal getBalance() {
        return balance;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }
}
