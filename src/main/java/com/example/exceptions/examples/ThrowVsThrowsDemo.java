package com.example.exceptions.examples;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;

/**
 * DEMONSTRATION ONLY.
 *
 * <ul>
 *   <li>{@code throw} is a STATEMENT inside a method body. It creates the problem: "stop now, here is
 *       an exception object". Followed by ONE object: {@code throw new X(...)}.</li>
 *   <li>{@code throws} is part of a METHOD SIGNATURE. It is a warning label: "calling me may produce
 *       these exceptions, and I am not handling them". Followed by one or more TYPES.</li>
 * </ul>
 * Analogy: {@code throw} is the referee blowing the whistle; {@code throws} is the sign at the
 * stadium entrance saying "matches here may be stopped".
 */
public class ThrowVsThrowsDemo {

    public static void main(String[] args) {
        // 1. throw: our own rule check
        try {
            validateCardPayment(new BigDecimal("5000.00"));
        } catch (IllegalArgumentException e) {
            System.out.println("throw demo  -> " + e.getMessage());
        }

        // 2. throws: the compiler forces us to catch (or declare) the checked IOException here
        try {
            loadExchangeRates("rates/GBP-EUR.csv");
        } catch (IOException e) {
            System.out.println("throws demo -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /** {@code throw}: WE decide something is wrong and raise the exception ourselves. */
    public static void validateCardPayment(BigDecimal amount) {
        BigDecimal singlePaymentLimit = new BigDecimal("2500.00");
        if (amount.compareTo(singlePaymentLimit) > 0) {
            throw new IllegalArgumentException(
                    "Card payment of " + amount + " exceeds the single-payment limit of " + singlePaymentLimit);
        }
        System.out.println("Card payment accepted: " + amount);
    }

    /**
     * {@code throws}: this method does not throw anything itself. FileReader may throw, and we
     * choose to PASS the problem to our caller instead of handling it here.
     */
    public static String loadExchangeRates(String path) throws IOException {
        try (FileReader reader = new FileReader(path)) {           // may throw FileNotFoundException
            char[] buffer = new char[64];
            int read = reader.read(buffer);                        // may throw IOException
            return read < 0 ? "" : new String(buffer, 0, read);
        }
    }

    /** Both together: a method that throws a CHECKED exception must also declare it. */
    public static void requireRatesFile(boolean present) throws FileNotFoundException {
        if (!present) {
            throw new FileNotFoundException("Exchange-rate file has not been delivered yet");
        }
    }
}
