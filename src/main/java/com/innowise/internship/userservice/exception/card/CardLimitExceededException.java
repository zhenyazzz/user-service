package com.innowise.internship.userservice.exception.card;

import java.util.UUID;

public class CardLimitExceededException extends RuntimeException {

    public CardLimitExceededException(UUID userId, int limit) {
        super("User " + userId + " cannot have more than " + limit + " cards");
    }

    public CardLimitExceededException(String message) {
        super(message);
    }
}
