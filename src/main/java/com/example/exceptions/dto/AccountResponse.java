package com.example.exceptions.dto;

import com.example.exceptions.model.Account;

import java.math.BigDecimal;

/** What the API returns for an account. The model object is never exposed directly. */
public record AccountResponse(String id, String ownerName, BigDecimal balance) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getId(), account.getOwnerName(), account.getBalance());
    }
}
