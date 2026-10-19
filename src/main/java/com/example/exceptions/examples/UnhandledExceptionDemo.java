package com.example.exceptions.examples;

import java.math.BigDecimal;

/**
 * DEMONSTRATION ONLY. Shows what happens when NOBODY catches an exception.
 *
 * <p>Expected console output:
 * <pre>
 * Starting end-of-day interest run...
 * Exception in thread "main" java.lang.IllegalStateException: Interest rate has not been configured
 *     at com.example.exceptions.examples.UnhandledExceptionDemo.currentInterestRate(UnhandledExceptionDemo.java:..)
 *     at com.example.exceptions.examples.UnhandledExceptionDemo.calculateInterest(UnhandledExceptionDemo.java:..)
 *     at com.example.exceptions.examples.UnhandledExceptionDemo.main(UnhandledExceptionDemo.java:..)
 * Process finished with exit code 1
 * </pre>
 *
 * The exception leaves main(), the thread's default handler prints the stack trace to System.err,
 * the main thread dies and the JVM exits with a non-zero code. "Interest run finished" never prints.
 */
public class UnhandledExceptionDemo {

    private static BigDecimal configuredRate; // never set: simulates missing configuration

    public static void main(String[] args) {
        System.out.println("Starting end-of-day interest run...");
        BigDecimal interest = calculateInterest(new BigDecimal("1000.00"));
        System.out.println("Interest run finished: " + interest);   // never reached
    }

    static BigDecimal calculateInterest(BigDecimal balance) {
        return balance.multiply(currentInterestRate());
    }

    static BigDecimal currentInterestRate() {
        if (configuredRate == null) {
            throw new IllegalStateException("Interest rate has not been configured");
        }
        return configuredRate;
    }
}
