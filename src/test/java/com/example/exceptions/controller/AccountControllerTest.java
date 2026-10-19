package com.example.exceptions.controller;

import com.example.exceptions.exception.AccountNotFoundException;
import com.example.exceptions.exception.InsufficientFundsException;
import com.example.exceptions.handler.GlobalExceptionHandler;
import com.example.exceptions.model.Account;
import com.example.exceptions.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer slice test: only the controller + GlobalExceptionHandler are loaded.
 * The service is a Mockito mock, so we decide exactly which exception it throws and check
 * that the handler translates it into the right HTTP status and JSON body.
 */
@WebMvcTest(AccountController.class)
@Import(GlobalExceptionHandler.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @Test
    void getAccountReturns200() throws Exception {
        when(accountService.getAccount("ACC-1001"))
                .thenReturn(new Account("ACC-1001", "Alice", new BigDecimal("500.00")));

        mockMvc.perform(get("/api/accounts/ACC-1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ACC-1001"))
                .andExpect(jsonPath("$.balance").value(500.00));
    }

    @Test
    void unknownAccountIsTranslatedTo404() throws Exception {
        when(accountService.getAccount("ACC-9999")).thenThrow(new AccountNotFoundException("ACC-9999"));

        mockMvc.perform(get("/api/accounts/ACC-9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Account ACC-9999 was not found"))
                .andExpect(jsonPath("$.path").value("/api/accounts/ACC-9999"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void insufficientFundsIsTranslatedTo409() throws Exception {
        when(accountService.withdraw(eq("ACC-1002"), any()))
                .thenThrow(new InsufficientFundsException("ACC-1002", new BigDecimal("100.00"), new BigDecimal("250.00")));

        mockMvc.perform(post("/api/accounts/ACC-1002/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 250.00}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Account ACC-1002 has insufficient funds for an amount of 250.00"));
    }

    @Test
    void unexpectedExceptionBecomes500WithoutInternalDetails() throws Exception {
        when(accountService.getAccount("ACC-1001")).thenThrow(new NullPointerException("secret internal detail"));

        mockMvc.perform(get("/api/accounts/ACC-1001"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again later."));
    }

    @Test
    void malformedJsonIs400() throws Exception {
        mockMvc.perform(post("/api/accounts/ACC-1001/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or is not valid JSON"));
    }
}
