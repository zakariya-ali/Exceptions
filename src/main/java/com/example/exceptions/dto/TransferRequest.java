package com.example.exceptions.dto;

import java.math.BigDecimal;

/** JSON body for POST /api/transfers. */
public record TransferRequest(String fromAccountId, String toAccountId, BigDecimal amount) {
}
