package com.innowise.internship.userservice.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
        String name,

        @Size(min = 1, max = 100, message = "Surname must be between 1 and 100 characters")
        String surname,

        @Past(message = "Birth date must be in the past")
        LocalDate birthDate
) {
}
