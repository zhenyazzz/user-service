package com.innowise.internship.userservice.dto.response;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
    String errorCode,
    String message,
    Instant timestamp,
    Map<String, String> details
) {}
