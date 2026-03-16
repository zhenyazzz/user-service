package com.innowise.internship.userservice.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.innowise.internship.userservice.model.enums.PaymentCardStatus;

public record PaymentCardResponse(
        UUID id,
        UUID userId,
        String number,
        String holder,
        LocalDate expirationDate,
        PaymentCardStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
