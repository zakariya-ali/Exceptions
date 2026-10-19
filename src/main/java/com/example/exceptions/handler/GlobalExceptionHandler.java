package com.example.exceptions.handler;

import com.example.exceptions.dto.ApiError;
import com.example.exceptions.exception.AccountNotFoundException;
import com.example.exceptions.exception.InsufficientFundsException;
import com.example.exceptions.exception.InvalidTransferException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * APPROACH B: one central place that turns Java exceptions into HTTP responses.
 *
 * <p>When a controller method throws, Spring MVC's ExceptionHandlerExceptionResolver searches
 * this class for the {@code @ExceptionHandler} whose type is the CLOSEST match to the thrown
 * exception (most specific type wins, regardless of method order here).
 *
 * <p>Policy: service THROWS, controller PROPAGATES, this class TRANSLATES and LOGS.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiError> handleAccountNotFound(AccountNotFoundException ex, HttpServletRequest request) {
        log.warn("Account not found: {}", ex.getAccountId());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ApiError> handleInsufficientFunds(InsufficientFundsException ex, HttpServletRequest request) {
        // The balance is logged for support staff, but never sent to the client.
        log.warn("Insufficient funds: account={}, balance={}, requested={}",
                ex.getAccountId(), ex.getBalance(), ex.getRequestedAmount());
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidTransferException.class)
    public ResponseEntity<ApiError> handleInvalidTransfer(InvalidTransferException ex, HttpServletRequest request) {
        log.warn("Invalid transfer: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /** Also catches NumberFormatException, because it is a subclass of IllegalArgumentException. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Bad request: {}", ex.getMessage());
        String message = ex instanceof NumberFormatException
                ? "Value is not a valid number"
                : ex.getMessage();
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    /** Malformed or missing JSON body. Without this it would fall into the 500 handler. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Unreadable request body: {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.BAD_REQUEST, "Request body is missing or is not valid JSON", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
                                                           HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method " + ex.getMethod() + " is not supported here", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "No endpoint exists at this path", request);
    }

    /**
     * Safety net for anything unexpected (bugs such as NullPointerException).
     * The FULL stack trace goes to the log; the client only gets a generic message.
     * We catch Exception, not Throwable: Errors (e.g. OutOfMemoryError) are not ours to handle.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again later.", request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(ApiError.of(status, message, request.getRequestURI()));
    }
}
