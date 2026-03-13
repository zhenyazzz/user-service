package com.innowise.internship.userservice.exception.user;

public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String email) {
        super("User with email already exists: " + email);
    }
}
