package com.velocitymotors.carbooking.common;

import java.time.Instant;
import com.velocitymotors.carbooking.payment.creditcard.PaymentRejectedException;
import com.velocitymotors.carbooking.payment.creditcard.PaymentValidationUnavailableException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class})
    ResponseEntity<Map<String, Object>> badRequest(Exception ex) {
        return error(HttpStatus.BAD_REQUEST, "Invalid booking request");
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    ResponseEntity<Map<String, Object>> notImplemented(UnsupportedOperationException ex) {
        return error(HttpStatus.NOT_IMPLEMENTED, ex.getMessage());
    }

    @ExceptionHandler(PaymentRejectedException.class)
    ResponseEntity<Map<String, Object>> paymentRejected(PaymentRejectedException ex) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(PaymentValidationUnavailableException.class)
    ResponseEntity<Map<String, Object>> paymentValidationFailed(PaymentValidationUnavailableException ex) {
        return error(HttpStatus.BAD_GATEWAY, "Credit-card validation could not be completed");
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "status", status.value(), "error", message, "timestamp", Instant.now().toString()));
    }
}
