package com.example.exceptions.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack tests: real controller -> real service -> real validator -> real handler.
 * No mocks. This proves the WHOLE propagation chain works end to end.
 *
 * <p>Each test creates its own accounts, so the tests do not depend on each other or on order.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BankingApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String createAccount(String owner, String openingBalance) throws Exception {
        String body = mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerName\":\"" + owner + "\",\"openingBalance\":" + openingBalance + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return json.get("id").asText();
    }

    @Test
    void successfulTransferReturns200() throws Exception {
        String from = createAccount("Carol", "300.00");
        String to = createAccount("Dan", "0");

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAccountId\":\"" + from + "\",\"toAccountId\":\"" + to + "\",\"amount\":120.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from.balance").value(180.00))
                .andExpect(jsonPath("$.to.balance").value(120.00));
    }

    @Test
    void withdrawalPropagatesFromValidatorToHandlerAs409() throws Exception {
        String id = createAccount("Erin", "50.00");

        mockMvc.perform(post("/api/accounts/" + id + "/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":75.00}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.path").value("/api/accounts/" + id + "/withdraw"));

        // The failed withdrawal did not change the balance.
        mockMvc.perform(get("/api/accounts/" + id))
                .andExpect(jsonPath("$.balance").value(50.00));
    }

    @Test
    void transferToSameAccountIs400() throws Exception {
        String id = createAccount("Frank", "10.00");

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAccountId\":\"" + id + "\",\"toAccountId\":\"" + id + "\",\"amount\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot transfer money from account " + id + " to itself"));
    }

    @Test
    void negativeDepositIs400() throws Exception {
        String id = createAccount("Grace", "10.00");

        mockMvc.perform(post("/api/accounts/" + id + "/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Amount must be greater than zero but was -1"));
    }

    @Test
    void uncheckedDemoReturns400() throws Exception {
        mockMvc.perform(get("/api/demo/unchecked"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Value is not a valid number"));
    }

    @Test
    void checkedDemoHandledLocallyReturns404() throws Exception {
        mockMvc.perform(get("/api/demo/checked"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No statement is available for account ACC-9999"));
    }

    @Test
    void checkedDemoReturnsStatementWhenFileExists() throws Exception {
        mockMvc.perform(get("/api/demo/checked").param("accountId", "ACC-1001"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.startsWith("STATEMENT FOR ACC-1001")));
    }

    @Test
    void unexpectedDemoReturns500() throws Exception {
        mockMvc.perform(get("/api/demo/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }
}
