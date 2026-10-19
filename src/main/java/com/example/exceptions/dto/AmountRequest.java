package com.example.exceptions.dto;

import java.math.BigDecimal;

/** JSON body for deposit and withdraw requests. */
public record AmountRequest(BigDecimal amount) {
}
