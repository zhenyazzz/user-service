package com.innowise.internship.userservice.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PaymentCardCreateRequest(
        @NotBlank(message = "Card number is required")
        @Pattern(regexp = "^\\d{16}$", message = "Card number must be exactly 16 digits")
        String number,

        @NotBlank(message = "Holder name is required")
        @Size(max = 255, message = "Holder name must not exceed 255 characters")
        String holder,

        @NotNull(message = "Expiration date is required")
        @Future(message = "Expiration date must be in the future")
        LocalDate expirationDate
) {
}
