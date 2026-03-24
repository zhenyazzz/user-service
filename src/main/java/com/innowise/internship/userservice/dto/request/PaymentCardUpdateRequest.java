package com.innowise.internship.userservice.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;

public record PaymentCardUpdateRequest(
        @Size(min = 1, max = 255, message = "Holder name must be between 1 and 255 characters")
        String holder,

        @Future(message = "Expiration date must be in the future")
        LocalDate expirationDate
) {
}
