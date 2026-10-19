package com.example.exceptions.examples;

/**
 * DEMONSTRATION ONLY. Basic try/catch with a banking calculation: monthly loan repayment.
 *
 * <ol>
 *   <li>try   - "attempt this code; something in here might throw".</li>
 *   <li>throw - when a statement throws, the REST of the try block is skipped immediately.</li>
 *   <li>catch - Java checks each catch, top to bottom, and runs the FIRST one whose type matches
 *       (the thrown object is an instance of that type or a subclass).</li>
 *   <li>after - execution continues with the first statement AFTER the whole try/catch.</li>
 * </ol>
 */
public class TryCatchDemo {

    public static void main(String[] args) {
        System.out.println(describeRepayment(12_000, 24));   // works
        System.out.println(describeRepayment(12_000, 0));    // fails, caught, program continues
        System.out.println("main finished normally");
    }

    public static String describeRepayment(int loanPounds, int months) {
        String result;
        try {
            System.out.println("  try: calculating for " + months + " months");
            int monthly = monthlyRepayment(loanPounds, months);  // BREAKPOINT: Step Into (F7) here
            result = "Monthly repayment: " + monthly;            // skipped when the line above throws
            System.out.println("  try: finished");
        } catch (ArithmeticException e) {
            System.out.println("  catch: " + e.getMessage());     // BREAKPOINT: inspect 'e' in Variables
            result = "Cannot calculate a repayment over " + months + " months";
        }
        System.out.println("  after try/catch: execution continues here");
        return result;
    }

    static int monthlyRepayment(int loanPounds, int months) {
        return loanPounds / months;                              // throws ArithmeticException: / by zero
    }
}
