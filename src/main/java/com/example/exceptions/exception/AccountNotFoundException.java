package com.example.exceptions.exception;

/**
 * Thrown when an account id does not exist.
 *
 * <p>UNCHECKED (extends RuntimeException): the caller usually cannot "recover" from a
 * wrong id inside the same request, so forcing every method to declare it would only
 * add noise. It travels up to the global handler, which turns it into HTTP 404.
 */
public class AccountNotFoundException extends RuntimeException {

    private final String accountId;

    public AccountNotFoundException(String accountId) {
        super("Account " + accountId + " was not found");
        this.accountId = accountId;
    }

    public String getAccountId() {
        return accountId;
    }
}
