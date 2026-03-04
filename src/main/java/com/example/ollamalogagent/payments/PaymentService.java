package com.example.ollamalogagent.payments;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PaymentService {

    public String processPayment() {
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                simulateDatabaseCall(attempt);
                return "ok";
            } catch (RuntimeException exception) {
                if (attempt == maxAttempts) {
                    throw exception;
                }
                log.warn("Payment attempt {} failed: {}", attempt, exception.getMessage());
            }
        }
        return "ok";
    }

    private void simulateDatabaseCall(int attempt) {
        if (attempt < 3) {
            throw new RuntimeException("java.sql.SQLTimeoutException: connection timeout after 5s");
        }
    }
}
