package com.example.exceptions.examples;

import com.example.exceptions.exception.AccountNotFoundException;
import com.example.exceptions.exception.InsufficientFundsException;

import java.io.FileNotFoundException;
import java.math.BigDecimal;

/**
 * DEMONSTRATION ONLY. Run with the green arrow next to main().
 *
 * <p>Part 1 prints the real superclass chain of several exceptions, so the hierarchy
 * (Throwable -> Exception / Error -> RuntimeException ...) comes from the JVM, not a slide.
 *
 * <p>Part 2 shows how an exception interrupts normal execution and propagates up the call stack:
 * main -> processPayment -> applyFee -> calculateFee (throws).
 */
public class ExceptionHierarchyDemo {

    public static void main(String[] args) {
        System.out.println("=== Part 1: the exception hierarchy, as the JVM sees it ===");
        printHierarchy(ArithmeticException.class);
        printHierarchy(FileNotFoundException.class);
        printHierarchy(StackOverflowError.class);
        printHierarchy(AccountNotFoundException.class);
        printHierarchy(InsufficientFundsException.class);

        System.out.println();
        System.out.println("=== Part 2: an exception interrupts execution and propagates ===");
        try {
            processPayment(new BigDecimal("100.00"), 0);
            System.out.println("This line is NEVER printed, because processPayment threw.");
        } catch (ArithmeticException e) {
            System.out.println("Caught in main: " + e);
            System.out.println("Stack trace (top = where it was thrown, bottom = where it started):");
            for (StackTraceElement frame : e.getStackTrace()) {
                System.out.println("    at " + frame);
            }
        }
        System.out.println("Program continues normally after the catch block.");
    }

    /** Walks getSuperclass() up to Object and prints the chain, plus checked/unchecked. */
    static void printHierarchy(Class<?> type) {
        StringBuilder chain = new StringBuilder(type.getSimpleName());
        Class<?> current = type.getSuperclass();
        while (current != null && current != Object.class) {
            chain.append(" -> ").append(current.getSimpleName());
            current = current.getSuperclass();
        }
        System.out.printf("%-90s [%s]%n", chain, classify(type));
    }

    /** The compiler's rule: RuntimeException and Error (and their subclasses) are unchecked. */
    public static String classify(Class<?> type) {
        if (Error.class.isAssignableFrom(type)) {
            return "Error - unchecked, do not catch";
        }
        if (RuntimeException.class.isAssignableFrom(type)) {
            return "unchecked";
        }
        return "checked";
    }

    static void processPayment(BigDecimal amount, int instalments) {
        System.out.println("processPayment: started");
        BigDecimal fee = applyFee(amount, instalments);
        System.out.println("processPayment: finished with fee " + fee); // skipped when applyFee throws
    }

    static BigDecimal applyFee(BigDecimal amount, int instalments) {
        System.out.println("applyFee: started");
        int feePerInstalment = calculateFee(instalments);
        System.out.println("applyFee: finished");                      // skipped when calculateFee throws
        return amount.add(BigDecimal.valueOf(feePerInstalment));
    }

    static int calculateFee(int instalments) {
        System.out.println("calculateFee: dividing 30 by " + instalments);
        return 30 / instalments;                                       // BREAKPOINT: ArithmeticException when 0
    }
}
