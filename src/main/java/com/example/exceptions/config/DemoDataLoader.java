package com.example.exceptions.config;

import com.example.exceptions.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;

/**
 * Creates two demo accounts on start-up so every curl example works immediately:
 * ACC-1001 (Alice, 500.00) and ACC-1002 (Bob, 100.00).
 *
 * <p>Disabled under the "test" profile so tests always start from an empty, predictable state.
 */
@Configuration
@Profile("!test")
public class DemoDataLoader {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    @Bean
    CommandLineRunner seedAccounts(AccountService accountService) {
        return args -> {
            accountService.createAccount("Alice Johnson", new BigDecimal("500.00"));
            accountService.createAccount("Bob Smith", new BigDecimal("100.00"));
            log.info("Demo accounts created: ACC-1001 (Alice, 500.00), ACC-1002 (Bob, 100.00)");
        };
    }
}
