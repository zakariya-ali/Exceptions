package com.example.exceptions.controller;

import com.example.exceptions.dto.ApiError;
import com.example.exceptions.service.StatementService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;

/**
 * DEMONSTRATION endpoints that make exceptions happen on purpose.
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private static final Logger log = LoggerFactory.getLogger(DemoController.class);

    private final StatementService statementService;

    public DemoController(StatementService statementService) {
        this.statementService = statementService;
    }

    /**
     * UNCHECKED demo. "ten-pounds" is not a number, so {@code new BigDecimal(...)} throws
     * NumberFormatException. Nothing here catches it, and the compiler did not force us to.
     * NumberFormatException extends IllegalArgumentException, so the global handler returns 400.
     *
     * <p>Try: /api/demo/unchecked?amount=25.50 (200) and /api/demo/unchecked (400)
     */
    @GetMapping("/unchecked")
    public String unchecked(@RequestParam(name = "amount", defaultValue = "ten-pounds") String amount) {
        BigDecimal parsed = new BigDecimal(amount);   // <- NumberFormatException thrown here
        return "Parsed amount: " + parsed;
    }

    /**
     * CHECKED demo, APPROACH A (local handling). loadStatement declares IOException, so the
     * compiler forces us to deal with it. Here we choose to catch it right where we can decide
     * the HTTP response.
     *
     * <p>Catch order matters: FileNotFoundException is a subclass of IOException, so it must come first.
     *
     * <p>Try: /api/demo/checked?accountId=ACC-1001 (200) and /api/demo/checked (404)
     */
    @GetMapping("/checked")
    public ResponseEntity<?> checked(@RequestParam(name = "accountId", defaultValue = "ACC-9999") String accountId,
                                     HttpServletRequest request) {
        try {
            String statement = statementService.loadStatement(accountId);
            return ResponseEntity.ok(statement);
        } catch (FileNotFoundException e) {
            // Expected case: no statement has been generated yet. Log briefly, no stack trace needed.
            log.info("No statement file for {}: {}", accountId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiError.of(HttpStatus.NOT_FOUND,
                            "No statement is available for account " + accountId, request.getRequestURI()));
        } catch (IOException e) {
            // Unexpected I/O failure: log WITH the exception so the stack trace is kept for developers.
            log.error("Failed to read statement for {}", accountId, e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiError.of(HttpStatus.SERVICE_UNAVAILABLE,
                            "Statements are temporarily unavailable", request.getRequestURI()));
        }
    }

    /**
     * Shows what happens to an exception that NO specific handler knows about: it falls through
     * to the catch-all handler and becomes a generic 500, with no internal details leaked.
     */
    @GetMapping("/unexpected")
    public String unexpected() {
        String branchCode = null;                     // simulates a bug: a value that was never set
        return "Branch: " + branchCode.toUpperCase(); // <- NullPointerException thrown here
    }
}
