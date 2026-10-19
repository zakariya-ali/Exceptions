package com.example.exceptions.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A bank account held in memory.
 *
 * <p>The balance can only change through {@link #credit} and {@link #debit}.
 * Business rules (positive amounts, sufficient funds) are checked by the service
 * layer BEFORE these methods are called.
 */
public class Account {

    private final String id;
    private final String ownerName;
    private BigDecimal balance;

    public Account(String id, String ownerName, BigDecimal openingBalance) {
        // Objects.requireNonNull fails fast with a clear NullPointerException message.
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.ownerName = Objects.requireNonNull(ownerName, "ownerName must not be null");
        this.balance = Objects.requireNonNull(openingBalance, "openingBalance must not be null");
    }

    public String getId() {
        return id;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void credit(BigDecimal amount) {
        balance = balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        balance = balance.subtract(amount);
    }
}
