package com.innowise.internship.userservice.exception.user;

import java.util.UUID;

public class UserIdAlreadyExistsException extends RuntimeException {

    public UserIdAlreadyExistsException(UUID userId) {
        super("User with id already exists: " + userId);
    }
}
