package com.example.exceptions.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;

/**
 * Reads account statements from text files. Used by the CHECKED exception demo.
 *
 * <p>File I/O can fail for reasons outside our control (missing file, permissions, disk errors),
 * so Java makes {@link IOException} a checked exception. This method declares it and lets the
 * caller decide what an I/O failure means for them.
 */
@Service
public class StatementService {

    private final Path statementsDir;

    public StatementService(@Value("${bank.statements.dir:statements}") String statementsDir) {
        this.statementsDir = Path.of(statementsDir);
    }

    /**
     * @throws FileNotFoundException if no statement file exists for the account
     * @throws IOException           if the file exists but cannot be read
     */
    public String loadStatement(String accountId) throws IOException {
        if (accountId == null || !accountId.matches("ACC-\\d+")) {
            // Prevents path tricks such as "../../secret" reaching the file system.
            throw new IllegalArgumentException("Invalid account id format: " + accountId);
        }
        Path file = statementsDir.resolve(accountId + ".txt");

        // try-with-resources: the reader is closed automatically, even if reading fails.
        // new FileReader(...) throws FileNotFoundException when the file is missing.
        try (BufferedReader reader = new BufferedReader(new FileReader(file.toFile()))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}
