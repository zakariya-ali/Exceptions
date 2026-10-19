package com.example.exceptions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the banking exception-handling demo.
 *
 * <p>Production-style code lives in controller/service/model/dto/exception/handler.
 * Demonstration-only code (plain Java main methods) lives in the {@code examples} package.
 */
@SpringBootApplication
public class ExceptionDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExceptionDemoApplication.class, args);
    }
}
