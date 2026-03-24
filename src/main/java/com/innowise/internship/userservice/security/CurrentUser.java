package com.innowise.internship.userservice.security;

import java.util.UUID;

public record CurrentUser(
    UUID userId, 
    String email,
    String role
) {}
