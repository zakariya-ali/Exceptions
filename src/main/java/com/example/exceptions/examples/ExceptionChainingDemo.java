package com.example.exceptions.examples;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.math.BigDecimal;

/**
 * DEMONSTRATION ONLY. Exception chaining and "what if a catch block throws?".
 *
 * <p>Chaining = wrapping a low-level exception inside a higher-level one, passing the original
 * as the CAUSE: {@code new PaymentProcessingException("...", e)}. The stack trace then shows
 * "Caused by: ...", so the root cause is never lost.
 *
 * <p>If a catch block throws a new exception, that new exception propagates and the original
 * is lost UNLESS you pass it as the cause. That is exactly what we do here.
 */
public class ExceptionChainingDemo {

    /** Higher-level, domain-specific exception. */
    public static class PaymentProcessingException extends RuntimeException {
        public PaymentProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static void main(String[] args) {
        try {
            settlePayment("PAY-77", new BigDecimal("120.00"));
        } catch (PaymentProcessingException e) {
            System.out.println("Caught : " + e.getMessage());
            System.out.println("Cause  : " + e.getCause());
            System.out.println("Root   : " + rootCause(e));
            System.out.println("\nFull stack trace (look for 'Caused by:'):");
            e.printStackTrace(System.out);
        }

        System.out.println("\n-- BAD: catch block throws WITHOUT the cause --");
        try {
            settlePaymentLosingCause();
        } catch (PaymentProcessingException e) {
            System.out.println("Cause is " + e.getCause() + " -> the original IOException is gone");
        }
    }

    public static void settlePayment(String paymentId, BigDecimal amount) {
        try {
            sendToClearingHouse(paymentId, amount);
        } catch (IOException e) {
            // Translate + chain: callers get a meaningful domain exception, developers keep the root cause.
            throw new PaymentProcessingException("Payment " + paymentId + " could not be settled", e);
        }
    }

    static void settlePaymentLosingCause() {
        try {
            sendToClearingHouse("PAY-78", BigDecimal.TEN);
        } catch (IOException e) {
            throw new PaymentProcessingException("Payment PAY-78 could not be settled", null); // anti-pattern
        }
    }

    static void sendToClearingHouse(String paymentId, BigDecimal amount) throws IOException {
        // A low-level network failure, itself caused by a timeout: a two-level chain.
        throw new IOException("Clearing-house gateway did not respond for " + paymentId,
                new SocketTimeoutException("TLS handshake timed out"));
    }

    public static Throwable rootCause(Throwable t) {
        Throwable current = t;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
