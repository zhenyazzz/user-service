package com.innowise.internship.userservice.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.innowise.internship.userservice.model.enums.UserStatus;

public record UserResponse(
        UUID id,
        String name,
        String surname,
        LocalDate birthDate,
        String email,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<PaymentCardResponse> paymentCards
) {
}
