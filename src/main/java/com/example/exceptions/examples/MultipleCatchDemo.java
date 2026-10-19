package com.example.exceptions.examples;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DEMONSTRATION ONLY. Parsing a payment instruction such as "ACC-1001:250.00" can fail in
 * DIFFERENT ways, and each way deserves a DIFFERENT response.
 *
 * <p>Ordering rule: put the MOST SPECIFIC exception first. NumberFormatException extends
 * IllegalArgumentException, so if IllegalArgumentException came first it would swallow every
 * NumberFormatException and the specific block would be unreachable. The compiler rejects that
 * with "exception NumberFormatException has already been caught".
 */
public class MultipleCatchDemo {

    private static final Map<String, BigDecimal> DAILY_LIMITS = Map.of("ACC-1001", new BigDecimal("1000.00"));

    public static void main(String[] args) {
        String[] instructions = {
                "ACC-1001:250.00",   // OK
                "ACC-1001:abc",      // NumberFormatException
                "ACC-1001:-5",       // IllegalArgumentException
                "ACC-1001",          // ArrayIndexOutOfBoundsException (no amount part)
                null                 // NullPointerException
        };
        for (String instruction : instructions) {
            System.out.println(instruction + " -> " + process(instruction));
        }
    }

    public static String process(String instruction) {
        try {
            String[] parts = instruction.split(":");                 // NPE if instruction is null
            String accountId = parts[0];
            BigDecimal amount = new BigDecimal(parts[1]);            // AIOOBE if no ':'; NFE if not a number
            if (amount.signum() <= 0) {
                throw new IllegalArgumentException("amount must be positive");
            }
            BigDecimal limit = DAILY_LIMITS.getOrDefault(accountId, BigDecimal.ZERO);
            return amount.compareTo(limit) <= 0 ? "ACCEPTED " + amount : "OVER LIMIT";
        } catch (NumberFormatException e) {                          // specific: subclass of IAE
            return "REJECTED: amount is not a number";
        } catch (IllegalArgumentException e) {                       // broader than NFE
            return "REJECTED: " + e.getMessage();
        } catch (ArrayIndexOutOfBoundsException | NullPointerException e) {
            // Multi-catch (Java 7+): same handling for unrelated types. 'e' is effectively final.
            return "REJECTED: malformed instruction (" + e.getClass().getSimpleName() + ")";
        }
    }
}
