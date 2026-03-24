package com.innowise.internship.userservice.exception.card;

import java.util.UUID;

public class PaymentCardNotFoundException extends RuntimeException {

    public PaymentCardNotFoundException(UUID id) {
        super("Payment card not found: " + id);
    }

    public PaymentCardNotFoundException(String message) {
        super(message);
    }
}
