package com.example.exceptions.examples;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DEMONSTRATION ONLY. Four common CHECKED exceptions.
 *
 * <p>Checked exceptions represent failures OUTSIDE our code's control: missing files, broken
 * networks, unavailable databases, missing classes. The compiler forces every caller to either
 * catch them or declare them with {@code throws}. Delete a {@code throws} clause below and the
 * file stops compiling: that is the whole point of "checked".
 */
public class CheckedExceptionExamples {

    public static void main(String[] args) {
        try {
            readPaymentFile(Path.of("payments", "2026-10-19-batch.csv"));
        } catch (FileNotFoundException e) {        // more specific first...
            System.out.println("FileNotFoundException: " + e.getMessage());
        } catch (IOException e) {                  // ...broader second
            System.out.println("IOException: " + e.getMessage());
        }

        try {
            writeThenReadDirectoryAsFile();
        } catch (IOException e) {
            System.out.println("IOException (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }

        try {
            connectToCoreBankingDatabase();
        } catch (SQLException e) {
            System.out.println("SQLException: " + e.getMessage() + " (SQLState=" + e.getSQLState() + ")");
        }

        try {
            loadPaymentGateway("com.example.payments.LegacySwiftGateway");
        } catch (ClassNotFoundException e) {
            System.out.println("ClassNotFoundException: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- FileNotFoundException
    /** FileNotFoundException is a subclass of IOException, thrown by the FileReader constructor. */
    public static String readPaymentFile(Path file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file.toFile()))) { // BREAKPOINT: throws if missing
            return reader.readLine();
        }
    }

    // ---------------------------------------------------------------- IOException
    /**
     * A general IOException: trying to READ a directory as if it were a file.
     * Files.readString throws an IOException subclass (the exact one depends on the OS).
     */
    public static String writeThenReadDirectoryAsFile() throws IOException {
        Path folder = Files.createTempDirectory("bank-export-");
        try {
            return Files.readString(folder);                           // BREAKPOINT: IOException - it's a directory
        } finally {
            Files.deleteIfExists(folder);                              // clean up either way
        }
    }

    // ---------------------------------------------------------------- SQLException
    /**
     * A genuine SQLException with no database needed: no JDBC driver on the classpath understands the
     * "jdbc:corebank:" URL, so DriverManager throws SQLException("No suitable driver found ...").
     * In real life the same exception type means a bad query, lost connection, constraint violation, etc.
     */
    public static void connectToCoreBankingDatabase() throws SQLException {
        try (Connection connection = DriverManager.getConnection("jdbc:corebank://db.internal:5432/accounts")) {
            System.out.println("Connected: " + connection);           // never reached
        }
    }

    // ---------------------------------------------------------------- ClassNotFoundException
    /**
     * Loading a class by NAME at runtime (plugins, drivers, configuration). The compiler cannot verify
     * the string, so a typo or missing JAR is only discovered at runtime, hence a checked exception.
     */
    public static Class<?> loadPaymentGateway(String className) throws ClassNotFoundException {
        return Class.forName(className);                               // BREAKPOINT: class does not exist
    }
}
