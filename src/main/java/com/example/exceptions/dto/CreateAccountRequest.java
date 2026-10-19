package com.example.exceptions.dto;

import java.math.BigDecimal;

/** JSON body for POST /api/accounts. */
public record CreateAccountRequest(String ownerName, BigDecimal openingBalance) {
}
