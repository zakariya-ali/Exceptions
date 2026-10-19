package com.example.exceptions.examples;

import java.util.ArrayList;
import java.util.List;

/**
 * DEMONSTRATION ONLY. {@code finally} runs whether the try block succeeds, throws, or returns.
 * Typical use: release a lock, close a connection, write an audit log entry.
 *
 * <p>When finally does NOT run:
 * <ul>
 *   <li>System.exit() / Runtime.halt() is called inside try or catch</li>
 *   <li>the JVM crashes or the process is killed (kill -9, power cut)</li>
 *   <li>the thread never leaves the try block (infinite loop, deadlock)</li>
 *   <li>it is a daemon thread and the JVM shuts down</li>
 * </ul>
 */
public class FinallyDemo {

    /** Records what happened so tests can check the order of events. */
    public static final List<String> AUDIT_LOG = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("-- successful payment --");
        processPayment("PAY-1", 50);
        System.out.println("-- failed payment --");
        try {
            processPayment("PAY-2", -10);
        } catch (IllegalArgumentException e) {
            System.out.println("main caught: " + e.getMessage());
        }
        System.out.println("-- return inside try --");
        System.out.println("returned: " + statusWithReturnInTry());
        System.out.println("Audit log: " + AUDIT_LOG);

        System.out.println("-- System.exit inside try (finally will NOT print) --");
        if (args.length > 0 && args[0].equals("--exit")) {
            exitInsideTry();
        } else {
            System.out.println("(run with program argument --exit to see this; it ends the JVM)");
        }
    }

    public static void processPayment(String paymentId, int amount) {
        AUDIT_LOG.add("lock " + paymentId);
        try {
            if (amount <= 0) {
                throw new IllegalArgumentException("Payment " + paymentId + " has invalid amount " + amount);
            }
            AUDIT_LOG.add("paid " + paymentId);
            System.out.println("  try: paid " + paymentId);
        } finally {
            AUDIT_LOG.add("unlock " + paymentId);                  // BREAKPOINT: reached in both cases
            System.out.println("  finally: lock released for " + paymentId);
        }
    }

    /** finally runs AFTER the return value is computed but BEFORE the method actually returns. */
    public static String statusWithReturnInTry() {
        try {
            return "COMPLETED";
        } finally {
            AUDIT_LOG.add("finally ran before return");
            // Never 'return' from finally: it would silently discard any exception from try.
        }
    }

    static void exitInsideTry() {
        try {
            System.out.println("  try: calling System.exit(0)");
            System.exit(0);
        } finally {
            System.out.println("  finally: you will never see this line");
        }
    }
}
