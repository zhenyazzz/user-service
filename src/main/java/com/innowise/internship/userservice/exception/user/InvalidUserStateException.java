package com.innowise.internship.userservice.exception.user;

public class InvalidUserStateException extends RuntimeException {

    public InvalidUserStateException(String message) {
        super(message);
    }
}
