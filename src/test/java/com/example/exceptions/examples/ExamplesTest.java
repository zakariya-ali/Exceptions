package com.example.exceptions.examples;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Proves that every demonstration behaves exactly as the presentation says it does.
 */
class ExamplesTest {

    @BeforeEach
    void clearLogs() {
        FinallyDemo.AUDIT_LOG.clear();
        TryWithResourcesDemo.AuditSession.EVENTS.clear();
    }

    @Test
    void uncheckedExamplesThrowTheAdvertisedTypes() {
        assertThrows(NullPointerException.class, UncheckedExceptionExamples::triggerNullPointer);
        assertThrows(ArithmeticException.class, UncheckedExceptionExamples::triggerArithmetic);
        assertThrows(ArrayIndexOutOfBoundsException.class, UncheckedExceptionExamples::triggerArrayIndexOutOfBounds);
        assertThrows(NumberFormatException.class, UncheckedExceptionExamples::triggerNumberFormat);
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class,
                UncheckedExceptionExamples::triggerIllegalArgument);
        assertThat(iae).isNotInstanceOf(NumberFormatException.class);
        assertThrows(IllegalStateException.class, UncheckedExceptionExamples::triggerIllegalState);
    }

    @Test
    void checkedExamplesThrowTheAdvertisedTypes() {
        assertThrows(FileNotFoundException.class,
                () -> CheckedExceptionExamples.readPaymentFile(Path.of("does-not-exist.csv")));
        assertThrows(IOException.class, CheckedExceptionExamples::writeThenReadDirectoryAsFile);
        assertThrows(SQLException.class, CheckedExceptionExamples::connectToCoreBankingDatabase);
        assertThrows(ClassNotFoundException.class,
                () -> CheckedExceptionExamples.loadPaymentGateway("com.example.payments.Missing"));
    }

    @Test
    void errorsAreRaisedInAControlledWay() {
        assertThrows(StackOverflowError.class,
                () -> ErrorExamples.compoundInterestBroken(BigDecimal.ONE, 1));
        assertThrows(OutOfMemoryError.class, ErrorExamples::requestImpossibleArray);
        assertEquals(new BigDecimal("1126.84"), ErrorExamples.compoundInterestFixed(new BigDecimal("1000.00"), 12));
    }

    @Test
    void hierarchyClassification() {
        assertEquals("checked", ExceptionHierarchyDemo.classify(IOException.class));
        assertEquals("unchecked", ExceptionHierarchyDemo.classify(IllegalStateException.class));
        assertThat(ExceptionHierarchyDemo.classify(OutOfMemoryError.class)).startsWith("Error");
    }

    @Test
    void tryCatchContinuesAfterCatch() {
        assertEquals("Monthly repayment: 500", TryCatchDemo.describeRepayment(12_000, 24));
        assertEquals("Cannot calculate a repayment over 0 months", TryCatchDemo.describeRepayment(12_000, 0));
    }

    @Test
    void multipleCatchPicksMostSpecificBlock() {
        assertEquals("ACCEPTED 250.00", MultipleCatchDemo.process("ACC-1001:250.00"));
        assertEquals("REJECTED: amount is not a number", MultipleCatchDemo.process("ACC-1001:abc"));
        assertEquals("REJECTED: amount must be positive", MultipleCatchDemo.process("ACC-1001:-5"));
        assertThat(MultipleCatchDemo.process("ACC-1001")).contains("ArrayIndexOutOfBoundsException");
        assertThat(MultipleCatchDemo.process(null)).contains("NullPointerException");
    }

    @Test
    void finallyRunsOnSuccessAndOnFailure() {
        FinallyDemo.processPayment("P1", 10);
        assertThrows(IllegalArgumentException.class, () -> FinallyDemo.processPayment("P2", 0));

        assertEquals(List.of("lock P1", "paid P1", "unlock P1", "lock P2", "unlock P2"), FinallyDemo.AUDIT_LOG);
    }

    @Test
    void finallyRunsBeforeReturn() {
        assertEquals("COMPLETED", FinallyDemo.statusWithReturnInTry());
        assertEquals(List.of("finally ran before return"), FinallyDemo.AUDIT_LOG);
    }

    @Test
    void throwAndThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> ThrowVsThrowsDemo.validateCardPayment(new BigDecimal("2500.01")));
        assertThrows(FileNotFoundException.class, () -> ThrowVsThrowsDemo.loadExchangeRates("no/such/file.csv"));
        assertThrows(FileNotFoundException.class, () -> ThrowVsThrowsDemo.requireRatesFile(false));
    }

    @Test
    void tryWithResourcesClosesInReverseOrderAndKeepsSuppressed() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> TryWithResourcesDemo.auditedTransfer(true));

        assertEquals("transfer failed in body", ex.getMessage());
        assertEquals(1, ex.getSuppressed().length);
        assertEquals("could not flush audit session database", ex.getSuppressed()[0].getMessage());
        assertEquals(List.of("open database", "open audit-log", "close audit-log", "close database"),
                TryWithResourcesDemo.AuditSession.EVENTS);
    }

    @Test
    void chainingKeepsTheRootCause() {
        ExceptionChainingDemo.PaymentProcessingException ex = assertThrows(
                ExceptionChainingDemo.PaymentProcessingException.class,
                () -> ExceptionChainingDemo.settlePayment("PAY-1", BigDecimal.ONE));

        assertThat(ex.getCause()).isInstanceOf(IOException.class);
        assertEquals("TLS handshake timed out", ExceptionChainingDemo.rootCause(ex).getMessage());
    }
}
