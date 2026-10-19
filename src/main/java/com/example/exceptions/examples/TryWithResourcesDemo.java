package com.example.exceptions.examples;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * DEMONSTRATION ONLY. try-with-resources (Java 7+).
 *
 * <p>Any object that implements {@link AutoCloseable} can be declared in {@code try (...)}.
 * Java then calls {@code close()} automatically when the block ends: normally, by return, or by
 * exception. Resources close in REVERSE order of declaration.
 *
 * <p>Why it beats manual close() in finally:
 * <ul>
 *   <li>you cannot forget to close;</li>
 *   <li>no null checks or nested try blocks in finally;</li>
 *   <li>if both the body AND close() throw, the body's exception is kept and the close() exception is
 *       attached as a SUPPRESSED exception instead of hiding the original problem.</li>
 * </ul>
 */
public class TryWithResourcesDemo {

    public static void main(String[] args) throws IOException {
        Path statement = Files.createTempFile("statement-", ".csv");
        Files.writeString(statement, "date,description,amount\n2026-10-01,Coffee,-3.20\n2026-10-02,Salary,2500.00\n");

        System.out.println("Total: " + sumStatement(statement));

        System.out.println("\n-- custom AutoCloseable: close order and suppressed exceptions --");
        try {
            auditedTransfer(true);
        } catch (IllegalStateException e) {
            System.out.println("main caught: " + e.getMessage());
            for (Throwable suppressed : e.getSuppressed()) {
                System.out.println("  suppressed: " + suppressed.getMessage());
            }
        }
        Files.deleteIfExists(statement);
    }

    /** Reads a CSV statement and totals the amount column. The reader is ALWAYS closed. */
    public static BigDecimal sumStatement(Path csv) throws IOException {
        BigDecimal total = BigDecimal.ZERO;
        try (BufferedReader reader = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
            String line = reader.readLine();                        // skip header
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",");
                total = total.add(new BigDecimal(columns[2]));      // BREAKPOINT: inspect 'columns'
            }
        }                                                           // reader.close() called here automatically
        return total;
    }

    /** A resource we control, so we can SEE close() happen. */
    public static final class AuditSession implements AutoCloseable {
        public static final List<String> EVENTS = new ArrayList<>();
        private final String name;
        private final boolean failOnClose;

        public AuditSession(String name, boolean failOnClose) {
            this.name = name;
            this.failOnClose = failOnClose;
            EVENTS.add("open " + name);
            System.out.println("  open " + name);
        }

        @Override
        public void close() {
            EVENTS.add("close " + name);
            System.out.println("  close " + name);
            if (failOnClose) {
                throw new IllegalStateException("could not flush audit session " + name);
            }
        }
    }

    public static void auditedTransfer(boolean failInBody) {
        try (AuditSession db = new AuditSession("database", true);
             AuditSession log = new AuditSession("audit-log", false)) {
            System.out.println("  body: moving money");
            if (failInBody) {
                throw new IllegalStateException("transfer failed in body");
            }
        }
        // Order printed: open database, open audit-log, body, close audit-log, close database
    }
}
