package com.example.exceptions.examples;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * DEMONSTRATION ONLY. Six common UNCHECKED exceptions (subclasses of RuntimeException).
 *
 * <p>The compiler does NOT force us to catch these. They usually mean a programming mistake
 * or invalid input, so the best fix is normally PREVENTION (validation), not catching.
 *
 * <p>Each pair of methods follows the same pattern:
 * {@code triggerXxx()} makes the exception happen, {@code safeXxx()} shows the prevention.
 * main() calls every trigger inside try/catch so all six run in one go.
 */
public class UncheckedExceptionExamples {

    public static void main(String[] args) {
        run("NullPointerException", UncheckedExceptionExamples::triggerNullPointer);
        System.out.println("   prevented -> " + safeCustomerEmail(null));

        run("ArithmeticException", UncheckedExceptionExamples::triggerArithmetic);
        System.out.println("   prevented -> " + safeSplitBill(90, 0));

        run("ArrayIndexOutOfBoundsException", UncheckedExceptionExamples::triggerArrayIndexOutOfBounds);
        System.out.println("   prevented -> " + safeLastTransaction(new BigDecimal[0]));

        run("NumberFormatException", UncheckedExceptionExamples::triggerNumberFormat);
        System.out.println("   prevented -> " + safeParseAmount("12,50"));

        run("IllegalArgumentException", UncheckedExceptionExamples::triggerIllegalArgument);
        System.out.println("   prevented -> validate the input before calling setScale (see safe method)");

        run("IllegalStateException", UncheckedExceptionExamples::triggerIllegalState);
        System.out.println("   prevented -> check card.isActive() before charging");
    }

    private static void run(String title, Runnable trigger) {
        System.out.println("\n== " + title + " ==");
        try {
            trigger.run();
            System.out.println("   (no exception?)");
        } catch (RuntimeException e) {
            // Demo-only catch so the next example can run. Real code would not catch these broadly.
            System.out.println("   caught: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- NullPointerException
    /** A customer without an email address on file. */
    record Customer(String name, String email) {
    }

    public static void triggerNullPointer() {
        Customer customer = new Customer("Alice", null);
        // Calling a method on a null reference. Java 14+ "helpful NPE" message names the null part:
        // Cannot invoke "String.toLowerCase()" because the return value of "...Customer.email()" is null
        String normalised = customer.email().toLowerCase();         // BREAKPOINT: NPE thrown here
        System.out.println(normalised);
    }

    public static String safeCustomerEmail(String email) {
        return Optional.ofNullable(email).map(String::toLowerCase).orElse("no-email-on-file");
    }

    // ---------------------------------------------------------------- ArithmeticException
    /** Splitting a restaurant bill between zero people: integer division by zero. */
    public static int triggerArithmeticSplit(int totalPence, int people) {
        return totalPence / people;                                   // BREAKPOINT: "/ by zero" when people == 0
    }

    public static void triggerArithmetic() {
        triggerArithmeticSplit(9000, 0);
    }

    public static String safeSplitBill(int totalPence, int people) {
        if (people <= 0) {
            return "cannot split between " + people + " people";
        }
        return "each pays " + (totalPence / people) + "p";
    }

    // ---------------------------------------------------------------- ArrayIndexOutOfBoundsException
    public static void triggerArrayIndexOutOfBounds() {
        BigDecimal[] lastThreeTransactions = {new BigDecimal("10.00"), new BigDecimal("25.00"), new BigDecimal("4.99")};
        // Classic off-by-one: valid indexes are 0, 1, 2. length is 3.
        BigDecimal latest = lastThreeTransactions[lastThreeTransactions.length]; // BREAKPOINT: index 3 out of bounds
        System.out.println(latest);
    }

    public static String safeLastTransaction(BigDecimal[] transactions) {
        if (transactions.length == 0) {
            return "no transactions yet";
        }
        return transactions[transactions.length - 1].toPlainString();
    }

    // ---------------------------------------------------------------- NumberFormatException
    public static void triggerNumberFormat() {
        String amountFromCsv = "12,50";                                // European decimal comma from an import file
        BigDecimal amount = new BigDecimal(amountFromCsv);             // BREAKPOINT: NumberFormatException
        System.out.println(amount);
    }

    /** Parsing user text is one of the few places where catching NumberFormatException is reasonable. */
    public static String safeParseAmount(String text) {
        try {
            return new BigDecimal(text.trim()).toPlainString();
        } catch (NumberFormatException e) {
            return "'" + text + "' is not a valid amount";
        }
    }

    // ---------------------------------------------------------------- IllegalArgumentException
    public static void triggerIllegalArgument() {
        BigDecimal amount = new BigDecimal("10.00");
        // A genuinely invalid argument that compiles fine: rounding mode is required to be a valid
        // RoundingMode ordinal (0..7). The JDK checks it and throws IllegalArgumentException("Invalid rounding mode").
        @SuppressWarnings("deprecation")
        BigDecimal rounded = amount.setScale(2, 42);                   // BREAKPOINT: IllegalArgumentException
        System.out.println(rounded);
    }

    /** Our own code throws IllegalArgumentException for bad arguments too (see TransactionValidator). */
    public static BigDecimal requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive but was " + amount);
        }
        return amount;
    }

    // ---------------------------------------------------------------- IllegalStateException
    /** A card that can only be charged while it is active. */
    static final class PaymentCard {
        private boolean active = false;

        void activate() {
            active = true;
        }

        boolean isActive() {
            return active;
        }

        void charge(BigDecimal amount) {
            // The ARGUMENT is fine; the OBJECT is in the wrong state for this call.
            if (!active) {
                throw new IllegalStateException("Card must be activated before it can be charged");
            }
            System.out.println("charged " + amount);
        }
    }

    public static void triggerIllegalState() {
        PaymentCard card = new PaymentCard();
        card.charge(new BigDecimal("19.99"));                          // BREAKPOINT: step into charge()
    }
}
