package com.example.exceptions.dto;

import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * The single, consistent JSON error shape returned by every error in this API.
 *
 * <p>It contains no stack trace, no exception class name and no internal details.
 *
 * @param timestamp when the error happened (UTC)
 * @param status    numeric HTTP status, e.g. 404
 * @param error     HTTP reason phrase, e.g. "Not Found"
 * @param message   safe, human-readable explanation
 * @param path      the request path that failed
 */
public record ApiError(Instant timestamp, int status, String error, String message, String path) {

    public static ApiError of(HttpStatus status, String message, String path) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path);
    }
}
