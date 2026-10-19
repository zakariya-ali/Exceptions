package com.example.exceptions.examples;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * DEMONSTRATION ONLY. Errors are NOT exceptions in the business sense: they mean the JVM itself is
 * in trouble. Application code should normally never catch them.
 *
 * <p>Both demos below are deliberately CONTROLLED and safe to run on a laptop:
 * <ul>
 *   <li>StackOverflowError: a recursion bug fills one thread's stack (by default about 512 KB to 1 MB).
 *       That memory is released as the stack unwinds, so it cannot harm the machine.</li>
 *   <li>OutOfMemoryError: we ask for an array LARGER than the JVM can ever create. The JVM refuses
 *       immediately, BEFORE allocating anything, so no real memory is used.</li>
 * </ul>
 * We catch them here ONLY to print what happened. That is a teaching exception, not a pattern to copy.
 */
public class ErrorExamples {

    public static void main(String[] args) {
        System.out.println("== StackOverflowError (controlled) ==");
        try {
            compoundInterestBroken(new BigDecimal("1000.00"), 12);
        } catch (StackOverflowError e) {
            System.out.println("caught " + e.getClass().getName()
                    + " - recursion with no base case filled the thread stack"
                    + " (the stack trace keeps only the last " + e.getStackTrace().length + " frames)");
        }
        System.out.println("fixed version: " + compoundInterestFixed(new BigDecimal("1000.00"), 12));

        System.out.println("\n== OutOfMemoryError (controlled) ==");
        try {
            requestImpossibleArray();
        } catch (OutOfMemoryError e) {
            System.out.println("caught " + e.getClass().getName() + ": " + e.getMessage());
        }
        System.out.println("Program still running because the JVM refused the allocation up front.");
    }

    /** BUG: the recursion never stops. months is never decreased, so there is no base case. */
    public static BigDecimal compoundInterestBroken(BigDecimal balance, int months) {
        BigDecimal next = balance.multiply(new BigDecimal("1.01")).setScale(2, RoundingMode.HALF_EVEN);
        return compoundInterestBroken(next, months);                   // BREAKPOINT: watch Frames panel grow
    }

    public static BigDecimal compoundInterestFixed(BigDecimal balance, int months) {
        if (months == 0) {
            return balance;                                            // base case
        }
        BigDecimal next = balance.multiply(new BigDecimal("1.01")).setScale(2, RoundingMode.HALF_EVEN);
        return compoundInterestFixed(next, months - 1);
    }

    /**
     * Java arrays are indexed by int, and HotSpot cannot create one with Integer.MAX_VALUE elements.
     * Throws OutOfMemoryError("Requested array size exceeds VM limit") without touching the heap.
     */
    public static long[] requestImpossibleArray() {
        return new long[Integer.MAX_VALUE];
    }
}
