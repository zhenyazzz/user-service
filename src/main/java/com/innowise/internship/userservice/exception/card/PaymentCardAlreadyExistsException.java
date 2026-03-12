package com.innowise.internship.userservice.exception.card;

public class PaymentCardAlreadyExistsException extends RuntimeException {

    public PaymentCardAlreadyExistsException(String message) {
        super(message);
    }
}
