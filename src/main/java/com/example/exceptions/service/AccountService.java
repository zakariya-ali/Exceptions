package com.example.exceptions.service;

import com.example.exceptions.exception.AccountNotFoundException;
import com.example.exceptions.exception.InsufficientFundsException;
import com.example.exceptions.model.Account;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Banking operations backed by an in-memory Map.
 *
 * <p>Exception policy for this layer:
 * <ul>
 *   <li>The service THROWS business exceptions when a rule is broken.</li>
 *   <li>The service does NOT catch them: it has no HTTP knowledge, so it lets them
 *       propagate to the web layer.</li>
 *   <li>The global handler TRANSLATES them into HTTP responses.</li>
 * </ul>
 *
 * <p>Money-moving methods are {@code synchronized} so that two concurrent requests cannot
 * both pass the funds check and overdraw an account. That is good enough for an in-memory demo.
 */
@Service
public class AccountService {

    private final Map<String, Account> accounts = new ConcurrentHashMap<>();
    private final AtomicInteger nextAccountNumber = new AtomicInteger(1001);
    private final TransactionValidator validator;

    public AccountService(TransactionValidator validator) {
        this.validator = validator;
    }

    public Account createAccount(String ownerName, BigDecimal openingBalance) {
        if (ownerName == null || ownerName.isBlank()) {
            throw new IllegalArgumentException("Owner name is required");
        }
        BigDecimal opening = openingBalance == null ? BigDecimal.ZERO : openingBalance;
        if (opening.signum() < 0) {
            throw new IllegalArgumentException("Opening balance cannot be negative but was " + opening);
        }
        if (opening.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Opening balance must not have more than 2 decimal places");
        }
        String id = "ACC-" + nextAccountNumber.getAndIncrement();
        Account account = new Account(id, ownerName.trim(), opening.setScale(2, RoundingMode.UNNECESSARY));
        accounts.put(id, account);
        return account;
    }

    /**
     * Lookup used when the caller EXPECTS the account to exist. A missing id is an error.
     */
    public Account getAccount(String accountId) {
        Account account = accounts.get(accountId);
        if (account == null) {
            throw new AccountNotFoundException(accountId);
        }
        return account;
    }

    /**
     * Lookup used when "not found" is a NORMAL outcome. No exception for ordinary control flow:
     * the caller simply checks the Optional.
     */
    public Optional<Account> findAccount(String accountId) {
        return Optional.ofNullable(accounts.get(accountId));
    }

    public BigDecimal getBalance(String accountId) {
        return getAccount(accountId).getBalance();
    }

    public synchronized Account deposit(String accountId, BigDecimal amount) {
        validator.requirePositiveAmount(amount);
        Account account = getAccount(accountId);
        account.credit(amount);
        return account;
    }

    /**
     * Note the {@code throws} clause: this method does not handle InsufficientFundsException,
     * it DECLARES that it may propagate it to its caller.
     */
    public synchronized Account withdraw(String accountId, BigDecimal amount) throws InsufficientFundsException {
        validator.requirePositiveAmount(amount);
        Account account = getAccount(accountId);
        validator.ensureSufficientFunds(account, amount);   // if this throws, the next line never runs
        account.debit(amount);
        return account;
    }

    /**
     * All checks run BEFORE any balance changes, so a failed transfer never leaves
     * the accounts half-updated.
     */
    public synchronized void transfer(String fromAccountId, String toAccountId, BigDecimal amount)
            throws InsufficientFundsException {
        validator.ensureDifferentAccounts(fromAccountId, toAccountId);
        validator.requirePositiveAmount(amount);
        Account from = getAccount(fromAccountId);
        Account to = getAccount(toAccountId);
        validator.ensureSufficientFunds(from, amount);

        from.debit(amount);
        to.credit(amount);
    }
}
