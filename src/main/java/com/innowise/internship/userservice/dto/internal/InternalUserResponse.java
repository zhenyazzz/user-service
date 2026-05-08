package com.innowise.internship.userservice.dto.internal;

import java.time.LocalDate;
import java.util.UUID;

public record InternalUserResponse(
        UUID id,
        String name,
        String surname,
        LocalDate birthDate,
        String email
) {
}
