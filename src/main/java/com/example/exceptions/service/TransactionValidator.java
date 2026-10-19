package com.example.exceptions.service;

import com.example.exceptions.exception.InsufficientFundsException;
import com.example.exceptions.exception.InvalidTransferException;
import com.example.exceptions.model.Account;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Helper that enforces money rules. It is the DEEPEST layer in the propagation demo:
 *
 * <pre>
 * AccountController.withdraw -> AccountService.withdraw -> TransactionValidator.ensureSufficientFunds -> throw
 * </pre>
 *
 * Validation is PREVENTION: we check the rule up front and throw a meaningful exception,
 * instead of letting the balance go negative and discovering the problem later.
 */
@Component
public class TransactionValidator {

    /**
     * @throws IllegalArgumentException if the amount is missing, zero/negative or has more than 2 decimals
     */
    public void requirePositiveAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero but was " + amount);
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Amount must not have more than 2 decimal places but was " + amount);
        }
    }

    /**
     * Checked exception: the signature FORCES callers to deal with insufficient funds.
     */
    public void ensureSufficientFunds(Account account, BigDecimal amount) throws InsufficientFundsException {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(account.getId(), account.getBalance(), amount);
        }
    }

    public void ensureDifferentAccounts(String fromAccountId, String toAccountId) {
        if (fromAccountId == null || toAccountId == null) {
            throw new InvalidTransferException("Both fromAccountId and toAccountId are required");
        }
        if (fromAccountId.equals(toAccountId)) {
            throw new InvalidTransferException("Cannot transfer money from account " + fromAccountId + " to itself");
        }
    }
}
