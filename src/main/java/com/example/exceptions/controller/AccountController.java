package com.example.exceptions.controller;

import com.example.exceptions.dto.AccountResponse;
import com.example.exceptions.dto.AmountRequest;
import com.example.exceptions.dto.CreateAccountRequest;
import com.example.exceptions.exception.InsufficientFundsException;
import com.example.exceptions.model.Account;
import com.example.exceptions.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;

/**
 * Account endpoints. APPROACH B (global handling): there is no try/catch here.
 *
 * <p>Any exception thrown by the service propagates out of these methods. Spring MVC then
 * looks for a matching {@code @ExceptionHandler} in {@code GlobalExceptionHandler}.
 * The controller stays focused on the "happy path".
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{id}")
    public AccountResponse getAccount(@PathVariable("id") String id) {
        // Unknown id -> AccountNotFoundException (unchecked) -> 404 via the global handler.
        return AccountResponse.from(accountService.getAccount(id));
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@RequestBody CreateAccountRequest request) {
        Account account = accountService.createAccount(request.ownerName(), request.openingBalance());
        return ResponseEntity
                .created(URI.create("/api/accounts/" + account.getId()))
                .body(AccountResponse.from(account));
    }

    @PostMapping("/{id}/deposit")
    public AccountResponse deposit(@PathVariable("id") String id, @RequestBody AmountRequest request) {
        return AccountResponse.from(accountService.deposit(id, request.amount()));
    }

    /**
     * InsufficientFundsException is CHECKED, so this method must either catch it or declare it.
     * We declare it ({@code throws}) because the global handler is the agreed place to translate it.
     */
    @PostMapping("/{id}/withdraw")
    public AccountResponse withdraw(@PathVariable("id") String id, @RequestBody AmountRequest request)
            throws InsufficientFundsException {
        Account account = accountService.withdraw(id, request.amount());
        return AccountResponse.from(account);
    }

    /** Small extra endpoint for the "check balance" requirement. */
    @GetMapping("/{id}/balance")
    public BigDecimal getBalance(@PathVariable("id") String id) {
        return accountService.getBalance(id);
    }
}
