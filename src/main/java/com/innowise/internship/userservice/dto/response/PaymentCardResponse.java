package com.innowise.internship.userservice.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentCardResponse(
        UUID id,
        UUID userId,
        String number,
        String holder,
        LocalDate expirationDate,
        Boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
