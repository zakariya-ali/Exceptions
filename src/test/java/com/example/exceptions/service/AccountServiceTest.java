package com.example.exceptions.service;

import com.example.exceptions.exception.AccountNotFoundException;
import com.example.exceptions.exception.InsufficientFundsException;
import com.example.exceptions.exception.InvalidTransferException;
import com.example.exceptions.model.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Plain unit tests: no Spring context, so they run in milliseconds and are easy to debug.
 * A fresh AccountService is created before EVERY test, so tests never affect each other.
 */
class AccountServiceTest {

    private AccountService service;
    private Account alice;
    private Account bob;

    @BeforeEach
    void setUp() {
        service = new AccountService(new TransactionValidator());
        alice = service.createAccount("Alice", new BigDecimal("500.00"));
        bob = service.createAccount("Bob", new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Successful transaction: a valid transfer moves money between accounts")
    void transferMovesMoney() throws InsufficientFundsException {
        service.transfer(alice.getId(), bob.getId(), new BigDecimal("150.00"));

        assertAll(
                () -> assertEquals(new BigDecimal("350.00"), service.getBalance(alice.getId())),
                () -> assertEquals(new BigDecimal("250.00"), service.getBalance(bob.getId())));
    }

    @Test
    @DisplayName("Successful deposit and withdrawal update the balance")
    void depositAndWithdraw() throws InsufficientFundsException {
        service.deposit(bob.getId(), new BigDecimal("25.50"));
        service.withdraw(bob.getId(), new BigDecimal("10.00"));

        assertEquals(new BigDecimal("115.50"), service.getBalance(bob.getId()));
    }

    @Test
    @DisplayName("Invalid withdrawal: more than the balance throws InsufficientFundsException")
    void withdrawMoreThanBalance() {
        // assertThrows runs the lambda, FAILS the test if nothing (or the wrong type) is thrown,
        // and RETURNS the exception so we can check its details.
        InsufficientFundsException ex = assertThrows(InsufficientFundsException.class,
                () -> service.withdraw(bob.getId(), new BigDecimal("100.01")));

        assertEquals(bob.getId(), ex.getAccountId());
        assertEquals(new BigDecimal("100.01"), ex.getRequestedAmount());
        assertEquals(new BigDecimal("100.00"), service.getBalance(bob.getId()), "balance must be unchanged");
    }

    @Test
    @DisplayName("Invalid withdrawal: a negative amount throws IllegalArgumentException")
    void withdrawNegativeAmount() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.withdraw(alice.getId(), new BigDecimal("-5.00")));

        assertThat(ex.getMessage()).contains("greater than zero");
    }

    @Test
    @DisplayName("Account that does not exist throws AccountNotFoundException")
    void unknownAccount() {
        AccountNotFoundException ex = assertThrows(AccountNotFoundException.class,
                () -> service.getAccount("ACC-0000"));

        assertEquals("Account ACC-0000 was not found", ex.getMessage());
    }

    @Test
    @DisplayName("findAccount returns an empty Optional instead of throwing")
    void findAccountDoesNotThrow() {
        assertThat(service.findAccount("ACC-0000")).isEmpty();
    }

    @Test
    @DisplayName("Failed transfer: insufficient funds leaves BOTH balances unchanged")
    void failedTransferIsAtomic() {
        assertThrows(InsufficientFundsException.class,
                () -> service.transfer(bob.getId(), alice.getId(), new BigDecimal("1000.00")));

        assertAll(
                () -> assertEquals(new BigDecimal("500.00"), service.getBalance(alice.getId())),
                () -> assertEquals(new BigDecimal("100.00"), service.getBalance(bob.getId())));
    }

    @Test
    @DisplayName("Failed transfer: same source and target throws InvalidTransferException")
    void transferToSameAccount() {
        InvalidTransferException ex = assertThrows(InvalidTransferException.class,
                () -> service.transfer(alice.getId(), alice.getId(), BigDecimal.TEN));

        assertThat(ex.getMessage()).contains("to itself");
    }

    @Test
    @DisplayName("Failed transfer: unknown target account throws AccountNotFoundException")
    void transferToUnknownAccount() {
        assertThrows(AccountNotFoundException.class,
                () -> service.transfer(alice.getId(), "ACC-4040", BigDecimal.TEN));
        assertEquals(new BigDecimal("500.00"), service.getBalance(alice.getId()));
    }
}
