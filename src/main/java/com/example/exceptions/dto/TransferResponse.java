package com.example.exceptions.dto;

/** Result of a successful transfer: both accounts after the money moved. */
public record TransferResponse(AccountResponse from, AccountResponse to) {
}
