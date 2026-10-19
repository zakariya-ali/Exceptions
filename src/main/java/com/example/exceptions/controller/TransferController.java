package com.example.exceptions.controller;

import com.example.exceptions.dto.AccountResponse;
import com.example.exceptions.dto.TransferRequest;
import com.example.exceptions.dto.TransferResponse;
import com.example.exceptions.exception.InsufficientFundsException;
import com.example.exceptions.service.AccountService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Transfer endpoint. Like AccountController it relies on global handling (Approach B).
 *
 * <p>Possible failures, all translated by GlobalExceptionHandler:
 * same account (400), bad amount (400), unknown account (404), not enough money (409).
 */
@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final AccountService accountService;

    public TransferController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public TransferResponse transfer(@RequestBody TransferRequest request) throws InsufficientFundsException {
        accountService.transfer(request.fromAccountId(), request.toAccountId(), request.amount());
        return new TransferResponse(
                AccountResponse.from(accountService.getAccount(request.fromAccountId())),
                AccountResponse.from(accountService.getAccount(request.toAccountId())));
    }
}
